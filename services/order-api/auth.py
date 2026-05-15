from jose import JWTError, jwt
import os
from fastapi import Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import NamedTuple

SECRET_KEY = os.getenv("SECRET_KEY", "change-me-in-production")
ALGORITHM = "HS256"

security = HTTPBearer()


class TokenUser(NamedTuple):
    id: int


def decode_access_token(token: str) -> dict | None:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except JWTError:
        return None


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> TokenUser:
    try:
        payload = decode_access_token(credentials.credentials)
        if payload is None:
            raise HTTPException(status_code=401, detail="Invalid token")
        user_id = int(payload.get("sub"))
        return TokenUser(id=user_id)
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")
