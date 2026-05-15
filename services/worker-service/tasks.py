# services/worker-service/tasks.py
from celery import Celery
import redis

app = Celery('tasks', broker='redis://redis:6379')

@app.task(name='process_order')
def process_order(order_id):
    """
    Run after order is created
    - Calculate shipping
    - Check inventory
    - Generate recommendations
    """
    order = get_order_from_db(order_id)
    
    # 1. Calculate shipping cost
    shipping_cost = calculate_shipping(
        order.destination,
        order.weight
    )
    
    # 2. Check inventory
    for item in order.items:
        if not has_inventory(item.product_id, item.quantity):
            send_notification(
                order.user_id,
                f"Item {item.product_id} out of stock"
            )
            return
    
    # 3. Generate recommendations
    recommendations = recommend_products(
        user_id=order.user_id,
        based_on=order.items
    )
    
    # 4. Update order status
    update_order_status(order_id, "PROCESSING")
    
    # 5. Publish event for notification service
    publish_event("ORDER_PROCESSED", {
        "order_id": order_id,
        "recommendations": recommendations
    })

@app.task(name='calculate_recommendations')
def calculate_recommendations(user_id):
    """
    Run periodically or after purchase
    - ML model or simple logic
    - What products to recommend to this user
    """
    user_history = get_user_purchase_history(user_id)
    recommendations = ml_model.predict(user_history)
    
    # Save to database
    save_recommendations(user_id, recommendations)