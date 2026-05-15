from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from models import Order
from schemas import OrderCreate, OrderResponse
import redis
import json
import os
import sys
import httpx

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging

logger = setup_logging(__name__)
REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379")
USER_SERVICE_URL = os.getenv("USER_SERVICE_URL", "http://user-service:8000")
cache = redis.Redis.from_url(REDIS_URL)

router = APIRouter(prefix="/orders", tags=["orders"])


async def validate_user_exists(user_id: int) -> bool:
    """Validate that user exists in User Service"""
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(
                f"{USER_SERVICE_URL}/users/{user_id}",
                headers={"Accept": "application/json"}
            )
            exists = response.status_code in [200, 404] and response.status_code == 200
            if exists:
                logger.info(f"User {user_id} validated in User Service")
            else:
                logger.warning(f"User {user_id} not found in User Service")
            return exists
    except Exception as e:
        logger.warning(f"Error validating user {user_id} with User Service: {e}")
        return True


@router.post("", response_model=OrderResponse, status_code=201)
async def create_order(body: OrderCreate, db: Session = Depends(get_db)):
    try:
        logger.info(f"Creating order for user {body.user_id}")

        user_exists = await validate_user_exists(body.user_id)
        if not user_exists:
            logger.warning(f"Cannot create order - user {body.user_id} not found")
            raise HTTPException(status_code=400, detail="User not found")

        new_order = Order(**body.model_dump())
        db.add(new_order)
        db.commit()
        db.refresh(new_order)

        cache.lpush("order_queue", json.dumps({
            "event": "ORDER_CREATED",
            "order_id": new_order.id,
            "user_id": new_order.user_id,
        }))

        logger.info(f"Order {new_order.id} created successfully")
        return OrderResponse.model_validate(new_order)
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating order: {e}", exc_info=True)
        raise


@router.get("/{order_id}", response_model=OrderResponse)
async def get_order(order_id: int, db: Session = Depends(get_db)):
    try:
        logger.info(f"Fetching order {order_id}")
        cached = cache.get(f"order:{order_id}")
        if cached:
            logger.info(f"Cache hit for order {order_id}")
            return json.loads(cached)

        logger.info(f"Cache miss for order {order_id}, querying database")
        order = db.query(Order).filter(Order.id == order_id).first()
        if not order:
            logger.warning(f"Order {order_id} not found")
            raise HTTPException(status_code=404, detail="Order not found")

        cache.setex(f"order:{order_id}", 3600, json.dumps(OrderResponse.model_validate(order).model_dump()))

        logger.info(f"Order {order_id} retrieved successfully")
        return OrderResponse.model_validate(order)
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error retrieving order {order_id}: {e}", exc_info=True)
        raise
