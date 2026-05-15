"""Integration test fixtures"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

SQLALCHEMY_DATABASE_URL = "sqlite:///./test_integration.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="session")
def test_db():
    """Create test database"""
    from services.user_service.database import Base as UserBase
    from services.order_api.database import Base as OrderBase

    UserBase.metadata.create_all(bind=engine)
    OrderBase.metadata.create_all(bind=engine)

    yield engine

    UserBase.metadata.drop_all(bind=engine)
    OrderBase.metadata.drop_all(bind=engine)


@pytest.fixture
def db_session(test_db):
    """Get database session"""
    connection = engine.connect()
    transaction = connection.begin()
    session = TestingSessionLocal(bind=connection)

    yield session

    session.close()
    transaction.rollback()
    connection.close()


@pytest.fixture
def user_client():
    """FastAPI test client for User Service"""
    from services.user_service.main import app
    from services.user_service.database import get_db

    def override_get_db():
        session = TestingSessionLocal()
        try:
            yield session
        finally:
            session.close()

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as client:
        yield client
    app.dependency_overrides.clear()


@pytest.fixture
def order_client():
    """FastAPI test client for Order API"""
    from services.order_api.main import app
    from services.order_api.database import get_db

    def override_get_db():
        session = TestingSessionLocal()
        try:
            yield session
        finally:
            session.close()

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as client:
        yield client
    app.dependency_overrides.clear()


@pytest.fixture
def test_user_data():
    """Test user data"""
    return {
        "email": "test@example.com",
        "password": "SecurePassword123",
        "full_name": "Test User",
        "phone": "+1234567890",
    }


@pytest.fixture
def test_order_data():
    """Test order data"""
    return {
        "user_id": 1,
        "destination": "123 Test Street, Test City",
        "weight": 2.5,
        "items": [{"product_id": 10, "quantity": 2}],
    }
