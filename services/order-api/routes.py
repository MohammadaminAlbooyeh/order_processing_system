from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import desc
from database import get_db
from models import Order
from schemas import OrderCreate, OrderResponse, OrderUpdate, OrderListResponse
import redis
import json
import os
import sys
import httpx
from datetime import datetime

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging

logger = setup_logging(__name__)

from shared.settings import get_settings
settings = get_settings()

REDIS_URL = settings.redis_url
USER_SERVICE_URL = settings.user_service_url
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


@router.get("", response_model=OrderListResponse)
async def list_orders(
    user_id: int | None = Query(None, description="Filter by user ID"),
    status: str | None = Query(None, description="Filter by status"),
    limit: int = Query(10, ge=1, le=100, description="Results per page"),
    offset: int = Query(0, ge=0, description="Number of results to skip"),
    db: Session = Depends(get_db),
):
    """List orders with pagination and filtering"""
    try:
        logger.info(f"Listing orders - user_id: {user_id}, status: {status}, limit: {limit}, offset: {offset}")

        query = db.query(Order)

        if user_id:
            query = query.filter(Order.user_id == user_id)

        if status:
            query = query.filter(Order.status == status)

        total = query.count()

        orders = query.order_by(desc(Order.created_at)).limit(limit).offset(offset).all()

        logger.info(f"Retrieved {len(orders)} orders out of {total}")

        return OrderListResponse(
            orders=[OrderResponse.model_validate(o) for o in orders],
            total=total,
            limit=limit,
            offset=offset,
        )
    except Exception as e:
        logger.error(f"Error listing orders: {e}", exc_info=True)
        raise


@router.put("/{order_id}", response_model=OrderResponse)
async def update_order(
    order_id: int,
    body: OrderUpdate,
    db: Session = Depends(get_db),
):
    """Update order status or destination"""
    try:
        logger.info(f"Updating order {order_id}")

        order = db.query(Order).filter(Order.id == order_id).first()
        if not order:
            logger.warning(f"Order {order_id} not found")
            raise HTTPException(status_code=404, detail="Order not found")

        if body.status:
            valid_transitions = {
                "pending": ["processing", "cancelled"],
                "processing": ["shipped", "cancelled"],
                "shipped": ["delivered", "cancelled"],
                "delivered": [],
                "cancelled": [],
            }

            if body.status not in valid_transitions.get(order.status, []):
                logger.warning(f"Invalid status transition for order {order_id}: {order.status} -> {body.status}")
                raise HTTPException(
                    status_code=400,
                    detail=f"Cannot transition from {order.status} to {body.status}"
                )

            old_status = order.status
            order.status = body.status
            logger.info(f"Order {order_id} status changed: {old_status} -> {body.status}")

        if body.destination:
            order.destination = body.destination
            logger.info(f"Order {order_id} destination updated")

        db.commit()
        db.refresh(order)

        cache.delete(f"order:{order_id}")

        logger.info(f"Order {order_id} updated successfully")
        return OrderResponse.model_validate(order)
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error updating order {order_id}: {e}", exc_info=True)
        raise


@router.delete("/{order_id}")
async def cancel_order(order_id: int, db: Session = Depends(get_db)):
    """Cancel an order"""
    try:
        logger.info(f"Cancelling order {order_id}")

        order = db.query(Order).filter(Order.id == order_id).first()
        if not order:
            logger.warning(f"Order {order_id} not found")
            raise HTTPException(status_code=404, detail="Order not found")

        if order.status in ["shipped", "delivered", "cancelled"]:
            logger.warning(f"Cannot cancel order {order_id} with status {order.status}")
            raise HTTPException(
                status_code=400,
                detail=f"Cannot cancel order with status {order.status}"
            )

        order.status = "cancelled"
        db.commit()
        db.refresh(order)

        cache.delete(f"order:{order_id}")

        try:
            import redis as r
            r_client = r.Redis.from_url(REDIS_URL)
            r_client.lpush("notification_queue", json.dumps({
                "type": "ORDER_CANCELLED",
                "order_id": order_id,
                "user_id": order.user_id,
            }))
        except Exception as e:
            logger.warning(f"Failed to publish ORDER_CANCELLED event: {e}")

        logger.info(f"Order {order_id} cancelled successfully")
        return {"detail": "Order cancelled successfully", "order_id": order_id}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error cancelling order {order_id}: {e}", exc_info=True)
        raise
