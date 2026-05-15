from fastapi import FastAPI
import redis
import json
import os
import asyncio
import sys
import logging

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging, setup_prometheus_metrics
from email import send_email
from sms import send_sms

logger = setup_logging(__name__)

REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379")
queue = redis.Redis.from_url(REDIS_URL)

app = FastAPI()
app = setup_prometheus_metrics(app)
event_listener_task = None


def format_recommendations(recommendations: list) -> str:
    if not recommendations:
        return "No recommendations yet."
    lines = [f"- {r}" for r in recommendations]
    return "\n".join(lines)


async def event_listener():
    logger.info("Starting notification event listener")
    while True:
        try:
            event = queue.lpop("notification_queue")
            if event:
                event_data = json.loads(event)
                logger.info(f"Processing event: {event_data['type']}")
                if event_data["type"] == "ORDER_PROCESSED":
                    await send_order_confirmation_email(event_data)
                    await send_sms_notification(event_data)
            await asyncio.sleep(0.1)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to decode event JSON: {e}")
        except Exception as e:
            logger.error(f"Error processing notification event: {e}", exc_info=True)
            await asyncio.sleep(1)


@app.on_event("startup")
async def startup():
    global event_listener_task
    event_listener_task = asyncio.create_task(event_listener())
    logger.info("Notification service started")


@app.on_event("shutdown")
async def shutdown():
    global event_listener_task
    if event_listener_task:
        event_listener_task.cancel()
        try:
            await event_listener_task
        except asyncio.CancelledError:
            logger.info("Event listener stopped")


async def send_order_confirmation_email(order_data: dict):
    subject = f"Order #{order_data['order_id']} Confirmed"
    body = f"""
Thank you for your order!
Order ID: {order_data['order_id']}
Estimated delivery: 3-5 business days

Recommended products for you:
{format_recommendations(order_data.get('recommendations', []))}
"""
    try:
        await asyncio.to_thread(
            send_email,
            order_data.get("user_email", ""),
            subject,
            body,
        )
        logger.info(f"Email sent for order {order_data['order_id']}")
    except Exception as e:
        logger.error(f"Failed to send confirmation email: {e}", exc_info=True)


async def send_sms_notification(order_data: dict):
    sms_body = (
        f"Order {order_data['order_id']} confirmed! "
        f"Track at: ecommerce.com/track/{order_data['order_id']}"
    )
    try:
        await asyncio.to_thread(
            send_sms,
            order_data.get("user_phone", ""),
            sms_body,
        )
        logger.info(f"SMS sent for order {order_data['order_id']}")
    except Exception as e:
        logger.error(f"Failed to send SMS notification: {e}", exc_info=True)


@app.get("/health")
async def health_check():
    return {"status": "healthy"}
