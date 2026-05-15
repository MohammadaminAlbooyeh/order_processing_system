import logging
import logging.handlers
import os
from datetime import datetime

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
LOG_FORMAT = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"


def setup_logging(name: str):
    logger = logging.getLogger(name)
    logger.setLevel(LOG_LEVEL)

    handler = logging.StreamHandler()
    formatter = logging.Formatter(LOG_FORMAT)
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    return logger


def setup_fastapi_logging(app):
    """Add request/response logging middleware to FastAPI app"""
    import time
    from fastapi import Request

    logger = logging.getLogger("fastapi.requests")

    @app.middleware("http")
    async def log_requests(request: Request, call_next):
        start_time = time.time()
        response = await call_next(request)
        process_time = time.time() - start_time
        logger.info(
            f"{request.method} {request.url.path} - "
            f"Status: {response.status_code} - "
            f"Duration: {process_time:.3f}s"
        )
        return response

    return app


def setup_prometheus_metrics(app):
    """Add Prometheus metrics to FastAPI app"""
    from prometheus_client import Counter, Histogram, generate_latest, REGISTRY
    from fastapi import Request

    request_count = Counter(
        "http_requests_total",
        "Total HTTP requests",
        ["method", "endpoint", "status_code"],
    )
    request_duration = Histogram(
        "http_request_duration_seconds",
        "HTTP request latency",
        ["method", "endpoint"],
    )

    @app.middleware("http")
    async def prometheus_middleware(request: Request, call_next):
        import time
        start_time = time.time()
        response = await call_next(request)
        process_time = time.time() - start_time

        request_count.labels(
            method=request.method,
            endpoint=request.url.path,
            status_code=response.status_code,
        ).inc()
        request_duration.labels(
            method=request.method,
            endpoint=request.url.path,
        ).observe(process_time)

        return response

    @app.get("/metrics")
    async def metrics():
        return generate_latest(REGISTRY)

    return app
