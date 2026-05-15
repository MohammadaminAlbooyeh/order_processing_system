import pytest
import json
from unittest.mock import patch, AsyncMock


@pytest.mark.asyncio
async def test_create_order_success(client):
    """Test successful order creation with user validation"""
    with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
        mock_validate.return_value = True
        response = client.post(
            "/orders",
            json={
                "user_id": 1,
                "destination": "123 Main St",
                "weight": 2.5,
                "items": [{"product_id": 10, "quantity": 2}],
            },
        )
    assert response.status_code == 201
    data = response.json()
    assert data["user_id"] == 1
    assert data["status"] == "pending"
    assert data["destination"] == "123 Main St"
    assert data["weight"] == 2.5


@pytest.mark.asyncio
async def test_create_order_user_not_found(client):
    """Test order creation fails when user doesn't exist"""
    with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
        mock_validate.return_value = False
        response = client.post(
            "/orders",
            json={
                "user_id": 999,
                "destination": "123 Main St",
                "weight": 2.5,
                "items": [{"product_id": 10, "quantity": 2}],
            },
        )
    assert response.status_code == 400
    assert "User not found" in response.json()["detail"]


@pytest.mark.asyncio
async def test_create_order_validation_error(client):
    """Test order creation with invalid data"""
    response = client.post(
        "/orders",
        json={
            "user_id": 1,
        },
    )
    assert response.status_code == 422


def test_get_order_success(client):
    """Test retrieving an order"""
    with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
        mock_validate.return_value = True
        create_response = client.post(
            "/orders",
            json={
                "user_id": 1,
                "destination": "123 Main St",
                "weight": 2.5,
                "items": [{"product_id": 10, "quantity": 2}],
            },
        )

    order_id = create_response.json()["id"]
    response = client.get(f"/orders/{order_id}")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == order_id
    assert data["user_id"] == 1


def test_get_order_not_found(client):
    """Test retrieving non-existent order"""
    response = client.get("/orders/999")
    assert response.status_code == 404
    assert response.json()["detail"] == "Order not found"


def test_get_order_from_cache(client):
    """Test retrieving order from cache"""
    with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
        mock_validate.return_value = True
        create_response = client.post(
            "/orders",
            json={
                "user_id": 1,
                "destination": "123 Main St",
                "weight": 2.5,
                "items": [{"product_id": 10, "quantity": 2}],
            },
        )

    order_id = create_response.json()["id"]
    response = client.get(f"/orders/{order_id}")
    assert response.status_code == 200

    response2 = client.get(f"/orders/{order_id}")
    assert response2.status_code == 200
    assert response2.json() == response.json()
