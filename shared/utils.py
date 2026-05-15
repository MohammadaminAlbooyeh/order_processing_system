from datetime import datetime, timezone


def format_currency(amount: float) -> str:
    return f"${amount:.2f}"


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def parse_order_id(order_id: int | str) -> int:
    if isinstance(order_id, str):
        return int(order_id)
    return order_id
