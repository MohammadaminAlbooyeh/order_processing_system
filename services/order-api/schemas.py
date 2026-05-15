from pydantic import BaseModel, Field, field_validator
from shared.schemas import OrderItem
from datetime import datetime


class OrderCreate(BaseModel):
    destination: str = Field(..., min_length=5, max_length=500, description="Delivery destination")
    weight: float = Field(default=0.0, ge=0, description="Weight in kg")
    items: list[OrderItem] = Field(..., min_items=1, description="At least one item required")

    @field_validator("destination")
    @classmethod
    def validate_destination(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Destination cannot be empty or whitespace")
        return v.strip()


class OrderResponse(BaseModel):
    id: int
    user_id: int
    status: str
    destination: str | None
    weight: float
    items: list[dict]
    created_at: datetime | None = None
    updated_at: datetime | None = None

    model_config = {"from_attributes": True}


class OrderUpdate(BaseModel):
    status: str | None = None
    destination: str | None = None

    @field_validator("status")
    @classmethod
    def validate_status(cls, v: str | None) -> str | None:
        valid_statuses = ["pending", "processing", "shipped", "delivered", "cancelled"]
        if v and v not in valid_statuses:
            raise ValueError(f"Status must be one of {valid_statuses}")
        return v

    @field_validator("destination")
    @classmethod
    def validate_destination(cls, v: str | None) -> str | None:
        if v is not None and (len(v) < 5 or len(v) > 500):
            raise ValueError("Destination must be between 5 and 500 characters")
        return v


class OrderListResponse(BaseModel):
    orders: list[OrderResponse]
    total: int
    limit: int
    offset: int
