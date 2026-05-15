"""Health check utilities for all services"""
from datetime import datetime
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)


class HealthCheckResponse:
    """Health check response model"""
    def __init__(self, status: str, timestamp: datetime, dependencies: Dict[str, Any] = None):
        self.status = status
        self.timestamp = timestamp
        self.dependencies = dependencies or {}

    def to_dict(self) -> Dict[str, Any]:
        return {
            "status": self.status,
            "timestamp": self.timestamp.isoformat(),
            "dependencies": self.dependencies,
        }


def check_database(db) -> Dict[str, Any]:
    """Check database connectivity"""
    try:
        db.execute("SELECT 1")
        return {"status": "healthy", "type": "postgresql"}
    except Exception as e:
        logger.error(f"Database health check failed: {e}")
        return {"status": "unhealthy", "type": "postgresql", "error": str(e)}


def check_redis(redis_client) -> Dict[str, Any]:
    """Check Redis connectivity"""
    try:
        redis_client.ping()
        return {"status": "healthy", "type": "redis"}
    except Exception as e:
        logger.error(f"Redis health check failed: {e}")
        return {"status": "unhealthy", "type": "redis", "error": str(e)}


def check_celery(celery_app) -> Dict[str, Any]:
    """Check Celery worker connectivity"""
    try:
        stats = celery_app.control.inspect().stats()
        if stats:
            return {"status": "healthy", "type": "celery", "workers": len(stats)}
        else:
            return {"status": "degraded", "type": "celery", "message": "No workers available"}
    except Exception as e:
        logger.error(f"Celery health check failed: {e}")
        return {"status": "unhealthy", "type": "celery", "error": str(e)}


def determine_overall_status(dependencies: Dict[str, Any]) -> str:
    """Determine overall status based on dependencies"""
    if not dependencies:
        return "healthy"

    unhealthy = [d for d in dependencies.values() if d.get("status") == "unhealthy"]
    degraded = [d for d in dependencies.values() if d.get("status") == "degraded"]

    if unhealthy:
        return "unhealthy"
    elif degraded:
        return "degraded"
    else:
        return "healthy"
