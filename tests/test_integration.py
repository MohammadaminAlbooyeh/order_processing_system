"""Integration tests for the complete system"""
import pytest
from unittest.mock import patch, AsyncMock


class TestUserServiceIntegration:
    """Integration tests for User Service"""

    def test_register_and_login_flow(self, user_client, test_user_data):
        """Test complete registration and login flow"""
        # Register user
        register_response = user_client.post(
            "/users/register",
            json=test_user_data,
        )
        assert register_response.status_code == 201
        register_data = register_response.json()
        assert "access_token" in register_data
        token = register_data["access_token"]

        # Login with same credentials
        login_response = user_client.post(
            "/users/login",
            json={
                "email": test_user_data["email"],
                "password": test_user_data["password"],
            },
        )
        assert login_response.status_code == 200
        login_data = login_response.json()
        assert login_data["user"]["email"] == test_user_data["email"]

    def test_protected_profile_endpoints(self, user_client, test_user_data):
        """Test protected profile endpoints"""
        # Register user
        register_response = user_client.post(
            "/users/register",
            json=test_user_data,
        )
        token = register_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # Get profile
        profile_response = user_client.get("/users/me", headers=headers)
        assert profile_response.status_code == 200
        profile = profile_response.json()
        assert profile["email"] == test_user_data["email"]

        # Update profile
        update_response = user_client.patch(
            "/users/me",
            headers=headers,
            json={"full_name": "Updated Name"},
        )
        assert update_response.status_code == 200
        updated = update_response.json()
        assert updated["full_name"] == "Updated Name"

    def test_get_user_by_id(self, user_client, test_user_data):
        """Test retrieving user by ID"""
        # Register user
        register_response = user_client.post(
            "/users/register",
            json=test_user_data,
        )
        user_id = register_response.json()["user"]["id"]

        # Get user by ID
        response = user_client.get(f"/users/{user_id}")
        assert response.status_code == 200
        user = response.json()
        assert user["id"] == user_id


class TestOrderServiceIntegration:
    """Integration tests for Order Service"""

    @pytest.mark.asyncio
    async def test_complete_order_lifecycle(self, order_client, test_order_data):
        """Test complete order creation, retrieval, and update"""
        # Create order with mocked user validation
        with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
            mock_validate.return_value = True

            # Create order
            create_response = order_client.post(
                "/orders",
                json=test_order_data,
            )
            assert create_response.status_code == 201
            order = create_response.json()
            order_id = order["id"]
            assert order["status"] == "pending"

            # List orders
            list_response = order_client.get(
                "/orders",
                params={"user_id": test_order_data["user_id"]},
            )
            assert list_response.status_code == 200
            list_data = list_response.json()
            assert list_data["total"] >= 1

            # Update order status
            update_response = order_client.put(
                f"/orders/{order_id}",
                json={"status": "processing"},
            )
            assert update_response.status_code == 200
            updated_order = update_response.json()
            assert updated_order["status"] == "processing"

            # Get order
            get_response = order_client.get(f"/orders/{order_id}")
            assert get_response.status_code == 200

    @pytest.mark.asyncio
    async def test_order_pagination(self, order_client, test_order_data):
        """Test order list pagination"""
        with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
            mock_validate.return_value = True

            # Create multiple orders
            for i in range(5):
                order_data = test_order_data.copy()
                order_data["destination"] = f"Address {i}"
                order_client.post("/orders", json=order_data)

            # Test pagination
            page1 = order_client.get("/orders", params={"limit": 2, "offset": 0})
            assert page1.status_code == 200
            assert len(page1.json()["orders"]) == 2

            page2 = order_client.get("/orders", params={"limit": 2, "offset": 2})
            assert page2.status_code == 200

    @pytest.mark.asyncio
    async def test_order_validation(self, order_client):
        """Test order input validation"""
        with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
            mock_validate.return_value = True

            # Test empty items list
            response = order_client.post(
                "/orders",
                json={
                    "user_id": 1,
                    "destination": "123 Test St",
                    "items": [],
                },
            )
            assert response.status_code == 422

            # Test short destination
            response = order_client.post(
                "/orders",
                json={
                    "user_id": 1,
                    "destination": "123",
                    "items": [{"product_id": 10, "quantity": 1}],
                },
            )
            assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_order_status_transitions(self, order_client, test_order_data):
        """Test valid and invalid order status transitions"""
        with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
            mock_validate.return_value = True

            # Create order
            create_response = order_client.post("/orders", json=test_order_data)
            order_id = create_response.json()["id"]

            # Valid transition: pending -> processing
            response = order_client.put(
                f"/orders/{order_id}",
                json={"status": "processing"},
            )
            assert response.status_code == 200

            # Invalid transition: processing -> pending
            response = order_client.put(
                f"/orders/{order_id}",
                json={"status": "pending"},
            )
            assert response.status_code == 400

    @pytest.mark.asyncio
    async def test_order_cancellation(self, order_client, test_order_data):
        """Test order cancellation"""
        with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
            mock_validate.return_value = True

            # Create order
            create_response = order_client.post("/orders", json=test_order_data)
            order_id = create_response.json()["id"]

            # Cancel order
            response = order_client.delete(f"/orders/{order_id}")
            assert response.status_code == 200

            # Get order and verify status
            get_response = order_client.get(f"/orders/{order_id}")
            assert get_response.json()["status"] == "cancelled"

            # Try to cancel again - should fail
            cancel_again = order_client.delete(f"/orders/{order_id}")
            assert cancel_again.status_code == 400


class TestHealthChecks:
    """Integration tests for health check endpoints"""

    def test_order_api_health_check(self, order_client):
        """Test Order API health check"""
        response = order_client.get("/health")
        assert response.status_code == 200
        health = response.json()
        assert health["status"] in ["healthy", "degraded"]
        assert "dependencies" in health

    def test_user_service_health_check(self, user_client):
        """Test User Service health check"""
        response = user_client.get("/health")
        assert response.status_code == 200
        health = response.json()
        assert health["status"] in ["healthy", "degraded"]
        assert "dependencies" in health


class TestCrossServiceValidation:
    """Integration tests for cross-service validation"""

    @pytest.mark.asyncio
    async def test_order_user_validation(self, order_client):
        """Test order creation validates user existence"""
        test_data = {
            "user_id": 999,
            "destination": "123 Test Street",
            "items": [{"product_id": 10, "quantity": 1}],
        }

        with patch('routes.validate_user_exists', new_callable=AsyncMock) as mock_validate:
            mock_validate.return_value = False
            response = order_client.post("/orders", json=test_data)
            assert response.status_code == 400
            assert "User not found" in response.json()["detail"]
