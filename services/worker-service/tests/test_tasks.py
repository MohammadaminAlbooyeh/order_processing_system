import pytest
from unittest.mock import MagicMock, patch
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))


def test_has_inventory_sufficient():
    """Test inventory check when product has sufficient stock"""
    from tasks import has_inventory
    assert has_inventory(10, 10) is True
    assert has_inventory(10, 50) is True


def test_has_inventory_insufficient():
    """Test inventory check when product has insufficient stock"""
    from tasks import has_inventory
    assert has_inventory(10, 51) is False
    assert has_inventory(14, 201) is False


def test_has_inventory_unknown_product():
    """Test inventory check for unknown product"""
    from tasks import has_inventory
    assert has_inventory(999, 10) is False


def test_recommend_products_based_on_history():
    """Test product recommendations based on purchase history"""
    from tasks import recommend_products
    history = [
        {"product_id": 10, "quantity": 2},
        {"product_id": 11, "quantity": 1},
    ]
    recommendations = recommend_products(user_id=1, based_on=history)
    assert isinstance(recommendations, list)
    assert len(recommendations) > 0
    assert 12 in recommendations or 13 in recommendations


def test_recommend_products_empty_history():
    """Test recommendations with empty purchase history"""
    from tasks import recommend_products
    recommendations = recommend_products(user_id=1, based_on=[])
    assert isinstance(recommendations, list)


def test_ml_model_predict_with_history():
    """Test ML model prediction with purchase history"""
    from tasks import ml_model
    history = [
        {"product_id": 10, "quantity": 3},
        {"product_id": 10, "quantity": 2},
        {"product_id": 11, "quantity": 1},
    ]
    predictions = ml_model.predict(history)
    assert isinstance(predictions, list)
    assert len(predictions) <= 5


def test_ml_model_predict_empty_history():
    """Test ML model prediction with empty history"""
    from tasks import ml_model
    predictions = ml_model.predict([])
    assert isinstance(predictions, list)
    assert len(predictions) > 0


def test_save_recommendations(mock_redis):
    """Test saving recommendations to cache"""
    with patch('tasks.r.Redis.from_url') as mock_redis_client:
        mock_redis_client.return_value = mock_redis
        from tasks import save_recommendations
        save_recommendations(1, [10, 11, 12])
        mock_redis.setex.assert_called()


@patch('tasks.get_order_from_db')
def test_process_order_success(mock_get_order):
    """Test successful order processing"""
    mock_get_order.return_value = {
        "id": 1,
        "user_id": 1,
        "destination": "123 Main St",
        "weight": 2.5,
        "items": [{"product_id": 10, "quantity": 2}],
    }

    with patch('tasks.update_order_status') as mock_update:
        with patch('tasks.publish_event') as mock_publish:
            from tasks import process_order
            process_order(1)
            mock_update.assert_called()
            mock_publish.assert_called()


@patch('tasks.get_order_from_db')
def test_process_order_not_found(mock_get_order):
    """Test order processing when order not found"""
    mock_get_order.return_value = None
    from tasks import process_order
    result = process_order(999)
    assert result is None


@patch('tasks.get_order_from_db')
@patch('tasks.has_inventory')
def test_process_order_out_of_stock(mock_inventory, mock_get_order):
    """Test order processing when item is out of stock"""
    mock_get_order.return_value = {
        "id": 1,
        "user_id": 1,
        "destination": "123 Main St",
        "weight": 2.5,
        "items": [{"product_id": 999, "quantity": 100}],
    }
    mock_inventory.return_value = False

    with patch('tasks.send_notification') as mock_notify:
        from tasks import process_order
        process_order(1)
        mock_notify.assert_called()


@patch('tasks.get_user_purchase_history')
@patch('tasks.ml_model')
def test_calculate_recommendations_success(mock_ml, mock_history):
    """Test recommendation calculation"""
    mock_history.return_value = [
        {"product_id": 10, "quantity": 2},
    ]
    mock_ml.predict.return_value = [11, 12]

    with patch('tasks.save_recommendations') as mock_save:
        from tasks import calculate_recommendations
        calculate_recommendations(1)
        mock_save.assert_called()
