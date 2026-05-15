from celery import Celery
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from shared.schemas import OrderItem
import os
import sys
import logging

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging
from shared.settings import get_settings

logger = setup_logging(__name__)
settings = get_settings()

DATABASE_URL = settings.database_url
REDIS_URL = settings.celery_broker_url

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
    inventory = {
        10: 50,
        11: 30,
        12: 100,
        13: 15,
        14: 200,
    }
    available = inventory.get(product_id, 0)
    return available >= quantity


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
    product_mapping = {
        10: [11, 12],
        11: [10, 13],
        12: [10, 14],
        13: [11, 14],
        14: [12, 13],
    }

    recommendations = set()
    for item in based_on:
        product_id = item.get("product_id")
        if product_id in product_mapping:
            recommendations.update(product_mapping[product_id])

    return list(recommendations)[:5]


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
    from sqlalchemy import text
    db = SessionLocal()
    try:
        rows = db.execute(
            text("SELECT items FROM orders WHERE user_id = :user_id ORDER BY created_at DESC LIMIT 10"),
            {"user_id": user_id}
        ).fetchall()
        history = []
        for row in rows:
            items = row[0] or []
            history.extend(items)
        return history
    except Exception as e:
        logger.error(f"Error fetching purchase history for user {user_id}: {e}")
        return []
    finally:
        db.close()


class MockMLModel:
    def predict(self, history):
        if not history:
            return [10, 11, 12]

        product_counts = {}
        for item in history:
            product_id = item.get("product_id")
            if product_id:
                product_counts[product_id] = product_counts.get(product_id, 0) + 1

        most_common = sorted(product_counts.items(), key=lambda x: x[1], reverse=True)[:3]
        product_mapping = {
            10: [11, 12],
            11: [10, 13],
            12: [10, 14],
            13: [11, 14],
            14: [12, 13],
        }

        recommendations = set()
        for product_id, _ in most_common:
            if product_id in product_mapping:
                recommendations.update(product_mapping[product_id])

        return list(recommendations)[:5]


ml_model = MockMLModel()


def save_recommendations(user_id: int, recommendations: list):
    import json
    import redis as r
    r_client = r.Redis.from_url(REDIS_URL)
    try:
        r_client.setex(
            f"user_recommendations:{user_id}",
            86400,
            json.dumps(recommendations)
        )
        logger.info(f"Saved {len(recommendations)} recommendations for user {user_id}")
    except Exception as e:
        logger.error(f"Error saving recommendations for user {user_id}: {e}")


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
