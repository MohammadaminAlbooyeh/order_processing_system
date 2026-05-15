from pydantic import BaseModel
from shared.schemas import OrderItem


class OrderCreate(BaseModel):
    user_id: int
    destination: str
    weight: float = 0.0
    items: list[OrderItem] = []


class OrderResponse(BaseModel):
    id: int
    user_id: int
    status: str
    destination: str | None
    weight: float
    items: list[dict]

    model_config = {"from_attributes": True}
