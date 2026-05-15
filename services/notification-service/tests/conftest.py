import pytest
from fastapi.testclient import TestClient
from unittest.mock import MagicMock, patch
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
from main import app


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def mock_email(monkeypatch):
    """Mock email sending"""
    mock = MagicMock()
    monkeypatch.setattr("email.send_email", mock)
    return mock


@pytest.fixture
def mock_sms(monkeypatch):
    """Mock SMS sending"""
    mock = MagicMock()
    monkeypatch.setattr("sms.send_sms", mock)
    return mock
