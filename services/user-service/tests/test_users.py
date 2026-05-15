import pytest


@pytest.fixture
def registered_user(client):
    """Register a user and return their details"""
    response = client.post(
        "/users/register",
        json={
            "email": "john@example.com",
            "password": "password123",
            "full_name": "John Doe",
            "phone": "+1234567890",
        },
    )
    data = response.json()
    return {
        "token": data["access_token"],
        "user_id": data["user"]["id"],
        "email": data["user"]["email"],
    }


def test_get_profile_authenticated(client, registered_user):
    """Test getting user profile with valid token"""
    headers = {"Authorization": f"Bearer {registered_user['token']}"}
    response = client.get("/users/me", headers=headers)
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == registered_user["email"]
    assert data["full_name"] == "John Doe"


def test_get_profile_no_token(client):
    """Test getting user profile without token"""
    response = client.get("/users/me")
    assert response.status_code == 403


def test_get_profile_invalid_token(client):
    """Test getting user profile with invalid token"""
    headers = {"Authorization": "Bearer invalid_token"}
    response = client.get("/users/me", headers=headers)
    assert response.status_code == 401


def test_get_user_by_id(client, registered_user):
    """Test getting user by ID"""
    response = client.get(f"/users/{registered_user['user_id']}")
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == registered_user["email"]


def test_get_user_not_found(client):
    """Test getting non-existent user"""
    response = client.get("/users/999")
    assert response.status_code == 404
    assert "User not found" in response.json()["detail"]


def test_update_profile(client, registered_user):
    """Test updating user profile"""
    headers = {"Authorization": f"Bearer {registered_user['token']}"}
    response = client.patch(
        "/users/me",
        headers=headers,
        json={
            "full_name": "Jane Doe",
            "phone": "+9876543210",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["full_name"] == "Jane Doe"
    assert data["phone"] == "+9876543210"


def test_update_profile_partial(client, registered_user):
    """Test partial profile update"""
    headers = {"Authorization": f"Bearer {registered_user['token']}"}
    response = client.patch(
        "/users/me",
        headers=headers,
        json={"full_name": "Jane Doe"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["full_name"] == "Jane Doe"
    assert data["phone"] == "+1234567890"  # Unchanged


def test_update_profile_no_token(client):
    """Test updating profile without token"""
    response = client.patch(
        "/users/me",
        json={"full_name": "Jane Doe"},
    )
    assert response.status_code == 403
