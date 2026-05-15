from celery import Celery
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from shared.schemas import OrderItem
import os
import sys
import logging

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging

logger = setup_logging(__name__)
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://ecommerce:password@postgres:5432/orders")
REDIS_URL = os.getenv("CELERY_BROKER_URL", "redis://redis:6379")

app = Celery("tasks", broker=REDIS_URL)

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_order_from_db(order_id: int):
    from sqlalchemy import text
    db = SessionLocal()
    try:
        row = db.execute(text("SELECT * FROM orders WHERE id = :id"), {"id": order_id}).mappings().first()
        return row
    finally:
        db.close()


def calculate_shipping(destination: str, weight: float) -> float:
    base_rate = 5.0
    per_kg_rate = 2.0
    return base_rate + (weight * per_kg_rate)


def has_inventory(product_id: int, quantity: int) -> bool:
    return True


def send_notification(user_id: int, message: str):
    import json
    import redis as r
    r_client = r.Redis.from_url(REDIS_URL)
    r_client.lpush("notification_queue", json.dumps({
        "type": "OUT_OF_STOCK",
        "user_id": user_id,
        "message": message,
    }))


def recommend_products(user_id: int, based_on: list) -> list:
    return []


def update_order_status(order_id: int, status: str):
    from sqlalchemy import text
    db = SessionLocal()
    try:
        db.execute(text("UPDATE orders SET status = :status WHERE id = :id"), {"status": status, "id": order_id})
        db.commit()
    finally:
        db.close()


def publish_event(event_type: str, data: dict):
    import json
    import redis as r
    r_client = r.Redis.from_url(REDIS_URL)
    r_client.lpush("notification_queue", json.dumps({
        "type": event_type,
        **data,
    }))


def get_user_purchase_history(user_id: int) -> list:
    return []


class MockMLModel:
    def predict(self, history):
        return []


ml_model = MockMLModel()


def save_recommendations(user_id: int, recommendations: list):
    pass


@app.task(name="process_order")
def process_order(order_id: int):
    try:
        logger.info(f"Processing order: {order_id}")
        order = get_order_from_db(order_id)
        if not order:
            logger.warning(f"Order not found: {order_id}")
            return

        shipping_cost = calculate_shipping(order["destination"], order["weight"])
        logger.info(f"Shipping cost for order {order_id}: ${shipping_cost:.2f}")

        for item in order.get("items") or []:
            if not has_inventory(item["product_id"], item["quantity"]):
                logger.warning(f"Inventory check failed for order {order_id}, item {item['product_id']}")
                send_notification(
                    order["user_id"],
                    f"Item {item['product_id']} out of stock",
                )
                return

        recommendations = recommend_products(
            user_id=order["user_id"],
            based_on=order.get("items") or [],
        )

        update_order_status(order_id, "PROCESSING")
        logger.info(f"Order {order_id} status updated to PROCESSING")

        publish_event("ORDER_PROCESSED", {
            "order_id": order_id,
            "recommendations": recommendations,
        })
        logger.info(f"Order {order_id} processed successfully")
    except Exception as e:
        logger.error(f"Error processing order {order_id}: {e}", exc_info=True)


@app.task(name="calculate_recommendations")
def calculate_recommendations(user_id: int):
    try:
        logger.info(f"Calculating recommendations for user: {user_id}")
        user_history = get_user_purchase_history(user_id)
        recommendations = ml_model.predict(user_history)
        save_recommendations(user_id, recommendations)
        logger.info(f"Recommendations calculated and saved for user: {user_id}")
    except Exception as e:
        logger.error(f"Error calculating recommendations for user {user_id}: {e}", exc_info=True)
