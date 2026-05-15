from pydantic_settings import BaseSettings
from pydantic import Field, field_validator


class Settings(BaseSettings):
    """Application configuration settings with validation"""

    # Database
    database_url: str = Field(
        default="postgresql://ecommerce:password@postgres:5432/orders",
        description="PostgreSQL database URL"
    )

    # Redis
    redis_url: str = Field(
        default="redis://redis:6379",
        description="Redis connection URL"
    )

    # Celery
    celery_broker_url: str = Field(
        default="redis://redis:6379",
        description="Celery broker URL"
    )

    # Security
    secret_key: str = Field(
        default="change-me-in-production",
        description="Secret key for JWT signing (MUST be changed in production)"
    )

    # Logging
    log_level: str = Field(
        default="INFO",
        description="Logging level"
    )

    # SMTP Configuration
    smtp_server: str = Field(
        default="smtp.gmail.com",
        description="SMTP server address"
    )

    smtp_port: int = Field(
        default=587,
        description="SMTP server port"
    )

    smtp_user: str = Field(
        default="",
        description="SMTP username (email)"
    )

    smtp_password: str = Field(
        default="",
        description="SMTP password"
    )

    # Twilio
    twilio_account_sid: str = Field(
        default="",
        description="Twilio account SID"
    )

    twilio_auth_token: str = Field(
        default="",
        description="Twilio auth token"
    )

    twilio_phone_number: str = Field(
        default="",
        description="Twilio phone number for SMS"
    )

    # Pusher
    pusher_app_id: str = Field(
        default="",
        description="Pusher app ID"
    )

    pusher_key: str = Field(
        default="",
        description="Pusher key"
    )

    pusher_secret: str = Field(
        default="",
        description="Pusher secret"
    )

    pusher_cluster: str = Field(
        default="mt1",
        description="Pusher cluster"
    )

    # Service URLs
    user_service_url: str = Field(
        default="http://user-service:8000",
        description="User Service URL for inter-service communication"
    )

    order_service_url: str = Field(
        default="http://order-api:8000",
        description="Order API Service URL"
    )

    # Jaeger
    jaeger_enabled: bool = Field(
        default=False,
        description="Enable Jaeger distributed tracing"
    )

    jaeger_host: str = Field(
        default="jaeger",
        description="Jaeger collector host"
    )

    jaeger_port: int = Field(
        default=6831,
        description="Jaeger collector port (UDP)"
    )

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False

    @field_validator("secret_key")
    @classmethod
    def validate_secret_key(cls, v: str) -> str:
        if v == "change-me-in-production":
            import warnings
            warnings.warn(
                "SECRET_KEY is set to default value. "
                "This is a security risk in production!"
            )
        return v

    @field_validator("database_url")
    @classmethod
    def validate_database_url(cls, v: str) -> str:
        if not v.startswith(("postgresql://", "sqlite://", "mysql://")):
            raise ValueError(
                "Invalid database URL. Must start with postgresql://, sqlite://, or mysql://"
            )
        return v


def get_settings() -> Settings:
    """Get application settings"""
    return Settings()
