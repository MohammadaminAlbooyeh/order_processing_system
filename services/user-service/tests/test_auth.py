import pytest


def test_register_success(client):
    """Test successful user registration"""
    response = client.post(
        "/users/register",
        json={
            "email": "john@example.com",
            "password": "password123",
            "full_name": "John Doe",
            "phone": "+1234567890",
        },
    )
    assert response.status_code == 201
    data = response.json()
    assert data["access_token"]
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == "john@example.com"
    assert data["user"]["full_name"] == "John Doe"


def test_register_duplicate_email(client):
    """Test registration with duplicate email"""
    client.post(
        "/users/register",
        json={
            "email": "john@example.com",
            "password": "password123",
            "full_name": "John Doe",
        },
    )

    response = client.post(
        "/users/register",
        json={
            "email": "john@example.com",
            "password": "different123",
            "full_name": "Jane Doe",
        },
    )
    assert response.status_code == 409
    assert "Email already registered" in response.json()["detail"]


def test_login_success(client):
    """Test successful login"""
    client.post(
        "/users/register",
        json={
            "email": "john@example.com",
            "password": "password123",
            "full_name": "John Doe",
        },
    )

    response = client.post(
        "/users/login",
        json={
            "email": "john@example.com",
            "password": "password123",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["access_token"]
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == "john@example.com"


def test_login_invalid_credentials(client):
    """Test login with invalid credentials"""
    client.post(
        "/users/register",
        json={
            "email": "john@example.com",
            "password": "password123",
            "full_name": "John Doe",
        },
    )

    response = client.post(
        "/users/login",
        json={
            "email": "john@example.com",
            "password": "wrongpassword",
        },
    )
    assert response.status_code == 401
    assert "Invalid credentials" in response.json()["detail"]


def test_login_user_not_found(client):
    """Test login for non-existent user"""
    response = client.post(
        "/users/login",
        json={
            "email": "nonexistent@example.com",
            "password": "password123",
        },
    )
    assert response.status_code == 401
    assert "Invalid credentials" in response.json()["detail"]
