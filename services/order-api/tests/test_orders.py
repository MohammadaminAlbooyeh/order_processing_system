from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_create_order(monkeypatch):
    class MockDB:
        def __init__(self):
            self.order = None

        def add(self, order):
            self.order = order

        def commit(self):
            self.order.id = 1

        def refresh(self, order):
            order.id = 1
            order.status = "pending"

        def close(self):
            pass

        def query(self, model):
            return self

        def filter(self, *args):
            return self

        def first(self):
            return None

    class MockRedis:
        def lpush(self, *args):
            pass

        def get(self, key):
            return None

        def setex(self, *args):
            pass

    import database
    import routes
    monkeypatch.setattr(database, "SessionLocal", lambda: MockDB())
    monkeypatch.setattr(routes, "cache", MockRedis())

    response = client.post("/orders", json={
        "user_id": 1,
        "destination": "123 Main St",
        "weight": 2.5,
        "items": [{"product_id": 10, "quantity": 2}],
    })

    assert response.status_code == 201
    data = response.json()
    assert data["user_id"] == 1
    assert data["status"] == "pending"
    assert data["destination"] == "123 Main St"


def test_get_order_not_found(monkeypatch):
    class MockDB:
        def query(self, model):
            return self

        def filter(self, *args):
            return self

        def first(self):
            return None

        def close(self):
            pass

    class MockRedis:
        def get(self, key):
            return None

        def setex(self, *args):
            pass

    import database
    import routes
    monkeypatch.setattr(database, "SessionLocal", lambda: MockDB())
    monkeypatch.setattr(routes, "cache", MockRedis())

    response = client.get("/orders/999")
    assert response.status_code == 404
    assert response.json()["detail"] == "Order not found"


def test_get_order_cached(monkeypatch):
    import json

    class MockDB:
        def query(self, model):
            return self

        def filter(self, *args):
            return self

        def first(self):
            return None

        def close(self):
            pass

    class MockRedis:
        def get(self, key):
            return json.dumps({
                "id": 1,
                "user_id": 1,
                "status": "pending",
                "destination": "123 Main St",
                "weight": 2.5,
                "items": [{"product_id": 10, "quantity": 2}],
            }).encode()

        def setex(self, *args):
            pass

    import database
    import routes
    monkeypatch.setattr(database, "SessionLocal", lambda: MockDB())
    monkeypatch.setattr(routes, "cache", MockRedis())

    response = client.get("/orders/1")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == 1
    assert data["status"] == "pending"
