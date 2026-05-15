import pytest
from unittest.mock import patch, MagicMock
import json


def test_health_check(client):
    """Test health check endpoint"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "healthy"}


def test_metrics_endpoint(client):
    """Test metrics endpoint exists"""
    response = client.get("/metrics")
    assert response.status_code == 200


def test_format_recommendations_empty():
    """Test formatting empty recommendations"""
    from main import format_recommendations
    result = format_recommendations([])
    assert result == "No recommendations yet."


def test_format_recommendations_with_items():
    """Test formatting recommendations with items"""
    from main import format_recommendations
    result = format_recommendations(["Product 1", "Product 2"])
    assert "Product 1" in result
    assert "Product 2" in result
    assert "-" in result


@pytest.mark.asyncio
async def test_send_order_confirmation_email_success(mock_email):
    """Test sending order confirmation email"""
    from main import send_order_confirmation_email
    order_data = {
        "order_id": 1,
        "user_email": "test@example.com",
        "recommendations": ["Product A"],
    }
    await send_order_confirmation_email(order_data)
    mock_email.assert_called()


@pytest.mark.asyncio
async def test_send_order_confirmation_email_no_email():
    """Test sending email with missing user email"""
    from main import send_order_confirmation_email
    order_data = {
        "order_id": 1,
        "recommendations": ["Product A"],
    }
    with patch("email.send_email") as mock_email:
        await send_order_confirmation_email(order_data)
        mock_email.assert_called_with("", "Order #1 Confirmed", pytest.ANY)


@pytest.mark.asyncio
async def test_send_sms_notification_success():
    """Test sending SMS notification"""
    from main import send_sms_notification
    order_data = {
        "order_id": 1,
        "user_phone": "+1234567890",
    }
    with patch("sms.send_sms") as mock_sms:
        await send_sms_notification(order_data)
        mock_sms.assert_called()


@pytest.mark.asyncio
async def test_send_sms_notification_no_phone():
    """Test sending SMS with missing phone"""
    from main import send_sms_notification
    order_data = {
        "order_id": 1,
    }
    with patch("sms.send_sms") as mock_sms:
        await send_sms_notification(order_data)
        mock_sms.assert_called_with("", pytest.ANY)


@pytest.mark.asyncio
async def test_event_listener_processes_order():
    """Test event listener processes ORDER_PROCESSED event"""
    from main import event_listener
    with patch("main.queue.lpop") as mock_lpop:
        with patch("main.send_order_confirmation_email") as mock_email:
            with patch("main.send_sms_notification") as mock_sms:
                mock_lpop.side_effect = [
                    json.dumps({
                        "type": "ORDER_PROCESSED",
                        "order_id": 1,
                        "user_email": "test@example.com",
                        "recommendations": [],
                    }).encode(),
                    None
                ]

                async def limited_listener():
                    event = mock_lpop("notification_queue")
                    if event:
                        event_data = json.loads(event)
                        if event_data["type"] == "ORDER_PROCESSED":
                            await mock_email(event_data)
                            await mock_sms(event_data)

                await limited_listener()
                mock_email.assert_called()
                mock_sms.assert_called()
