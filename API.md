# API Documentation

## Order API (`http://localhost:8001`)

### POST /orders
Create a new order.

**Request:**
```json
{
  "user_id": 1,
  "destination": "123 Main St",
  "weight": 2.5,
  "items": [{"product_id": 10, "quantity": 2}]
}
```

**Response (201):**
```json
{
  "id": 1,
  "user_id": 1,
  "status": "pending",
  "destination": "123 Main St",
  "weight": 2.5,
  "items": [{"product_id": 10, "quantity": 2}]
}
```

### GET /orders/{order_id}
Retrieve an order by ID. Results are cached in Redis for 1 hour.

**Response (200):**
```json
{
  "id": 1,
  "user_id": 1,
  "status": "pending",
  "destination": "123 Main St",
  "weight": 2.5,
  "items": [{"product_id": 10, "quantity": 2}]
}
```

**Response (404):** `{"detail": "Order not found"}`

---

## User Service (`http://localhost:8002`)

### POST /users/register
Register a new user.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securepassword",
  "full_name": "John Doe",
  "phone": "+1234567890"
}
```

**Response (201):**
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "full_name": "John Doe",
    "phone": "+1234567890",
    "is_active": true
  }
}
```

**Response (409):** `{"detail": "Email already registered"}`

### POST /users/login
Authenticate and receive a JWT.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securepassword"
}
```

**Response (200):**
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "full_name": "John Doe",
    "phone": "+1234567890",
    "is_active": true
  }
}
```

**Response (401):** `{"detail": "Invalid credentials"}`

### GET /users/me
Get the authenticated user's profile. Requires `Authorization: Bearer <token>` header.

**Response (200):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "full_name": "John Doe",
  "phone": "+1234567890",
  "is_active": true
}
```

### PATCH /users/me
Update the authenticated user's profile.

**Request:**
```json
{
  "full_name": "Jane Doe",
  "phone": "+0987654321"
}
```

**Response (200):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "full_name": "Jane Doe",
  "phone": "+0987654321",
  "is_active": true
}
```
