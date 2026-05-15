from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from routes import router
from database import engine, Base
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging, setup_fastapi_logging, setup_prometheus_metrics
from shared.settings import get_settings
from shared.tracing import setup_all_instrumentation

logger = setup_logging(__name__)
settings = get_settings()

logger.info(f"Initializing User Service with database: {settings.database_url}")
Base.metadata.create_all(bind=engine)

app = FastAPI(title="User Service")
app.include_router(router)
app = setup_fastapi_logging(app)
app = setup_prometheus_metrics(app)
setup_all_instrumentation(app, "user-service", settings)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.warning(f"Validation error on {request.url.path}: {exc.errors()}")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors()},
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception on {request.method} {request.url.path}: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error"},
    )


@app.on_event("startup")
async def startup():
    logger.info("User Service started")


@app.on_event("shutdown")
async def shutdown():
    logger.info("User Service shutting down")
