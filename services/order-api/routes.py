# services/order-api/routes.py
from fastapi import FastAPI, HTTPException
from sqlalchemy import create_engine
import redis
import json

app = FastAPI()
db = create_engine("postgresql://...")
cache = redis.Redis(host='redis', port=6379)

@app.post("/orders")
async def create_order(order_data: OrderSchema):
    """
    User creates an order
    """
    # 1. Validate request
    # 2. Save to PostgreSQL
    new_order = Order(**order_data.dict())
    db.add(new_order)
    db.commit()
    
    # 3. Publish event to queue for async processing
    # This tells workers: "Hey, new order created!"
    cache.lpush("order_queue", json.dumps({
        "event": "ORDER_CREATED",
        "order_id": new_order.id,
        "user_id": new_order.user_id
    }))
    
    return {"order_id": new_order.id, "status": "pending"}

@app.get("/orders/{order_id}")
async def get_order(order_id: int):
    """
    User checks order status
    """
    # Check cache first (fast)
    cached = cache.get(f"order:{order_id}")
    if cached:
        return json.loads(cached)
    
    # Cache miss, query database
    order = db.query(Order).filter(Order.id == order_id).first()
    
    # Cache it for next time
    cache.setex(f"order:{order_id}", 3600, json.dumps(order))
    
    return order