from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from routes import router
from database import engine, Base
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging, setup_fastapi_logging, setup_prometheus_metrics

logger = setup_logging(__name__)
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Order API")
app.include_router(router)
app = setup_fastapi_logging(app)
app = setup_prometheus_metrics(app)


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
    logger.info("Order API service started")


@app.on_event("shutdown")
async def shutdown():
    logger.info("Order API service shutting down")
