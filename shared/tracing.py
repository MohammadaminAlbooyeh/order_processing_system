"""
Distributed tracing configuration using OpenTelemetry and Jaeger
"""
from opentelemetry import trace, metrics
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import SERVICE_NAME, Resource
import logging

logger = logging.getLogger(__name__)


def setup_tracing(service_name: str, jaeger_host: str = "jaeger", jaeger_port: int = 6831):
    try:
        from opentelemetry.exporter.jaeger.thrift import JaegerExporter
        jaeger_exporter = JaegerExporter(
            agent_host_name=jaeger_host,
            agent_port=jaeger_port,
        )

        trace_provider = TracerProvider(
            resource=Resource.create({SERVICE_NAME: service_name})
        )
        trace_provider.add_span_processor(
            BatchSpanProcessor(jaeger_exporter)
        )

        trace.set_tracer_provider(trace_provider)
        logger.info(f"Jaeger tracing initialized for {service_name}")

        return trace_provider
    except Exception as e:
        logger.warning(f"Failed to initialize Jaeger tracing: {e}. Tracing disabled.")
        return None


def instrument_fastapi(app, service_name: str = "fastapi-service"):
    try:
        from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
        FastAPIInstrumentor.instrument_app(app, service_name=service_name)
        logger.info("FastAPI instrumented with OpenTelemetry")
    except Exception as e:
        logger.warning(f"Failed to instrument FastAPI: {e}")


def instrument_sqlalchemy(engine):
    try:
        from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor
        SQLAlchemyInstrumentor().instrument(engine=engine)
        logger.info("SQLAlchemy instrumented with OpenTelemetry")
    except Exception as e:
        logger.warning(f"Failed to instrument SQLAlchemy: {e}")


def instrument_requests():
    try:
        from opentelemetry.instrumentation.requests import RequestsInstrumentor
        RequestsInstrumentor().instrument()
        logger.info("Requests library instrumented with OpenTelemetry")
    except Exception as e:
        logger.warning(f"Failed to instrument requests: {e}")


def instrument_redis():
    try:
        from opentelemetry.instrumentation.redis import RedisInstrumentor
        RedisInstrumentor().instrument()
        logger.info("Redis instrumented with OpenTelemetry")
    except Exception as e:
        logger.warning(f"Failed to instrument Redis: {e}")


def setup_all_instrumentation(app, service_name: str, settings):
    """Set up all OpenTelemetry instrumentation"""
    if settings.jaeger_enabled:
        setup_tracing(service_name, settings.jaeger_host, settings.jaeger_port)
        instrument_fastapi(app, service_name)
        instrument_requests()
        instrument_redis()
        logger.info(f"Full OpenTelemetry instrumentation enabled for {service_name}")
    else:
        logger.info("OpenTelemetry tracing disabled (JAEGER_ENABLED=false)")
