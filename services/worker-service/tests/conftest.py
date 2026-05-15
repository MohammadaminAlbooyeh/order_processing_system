import pytest
from unittest.mock import MagicMock, patch
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))


@pytest.fixture
def mock_db(monkeypatch):
    """Mock database session"""
    db = MagicMock()
    return db


@pytest.fixture
def mock_redis(monkeypatch):
    """Mock Redis client"""
    redis = MagicMock()
    return redis
