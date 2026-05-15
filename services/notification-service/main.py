# services/notification-service/main.py
from fastapi import FastAPI
import redis
import smtplib
import json

app = FastAPI()
queue = redis.Redis(host='redis')

# Listen to events from other services
@app.on_event("startup")
async def listen_to_events():
    while True:
        event = queue.lpop("notification_queue")
        if event:
            event_data = json.loads(event)
            
            if event_data["type"] == "ORDER_PROCESSED":
                send_order_confirmation_email(event_data)
                send_sms_notification(event_data)

def send_order_confirmation_email(order_data):
    """
    Send email to customer
    """
    subject = f"Order #{order_data['order_id']} Confirmed"
    body = f"""
    Thank you for your order!
    Order ID: {order_data['order_id']}
    Estimated delivery: 3-5 business days
    
    Recommended products for you:
    {format_recommendations(order_data['recommendations'])}
    """
    
    # SMTP to send email
    send_email(
        to=order_data['user_email'],
        subject=subject,
        body=body
    )

def send_sms_notification(order_data):
    """
    Send SMS via Twilio or similar
    """
    sms_body = f"Order {order_data['order_id']} confirmed! Track at: ecommerce.com/track/{order_data['order_id']}"
    
    # Twilio API call
    send_sms(
        phone=order_data['user_phone'],
        message=sms_body
    )