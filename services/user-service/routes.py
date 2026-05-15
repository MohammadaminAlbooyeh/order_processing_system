from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from pydantic import BaseModel, EmailStr
from database import get_db
from models import User
from auth import hash_password, verify_password, create_access_token, decode_access_token
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging

logger = setup_logging(__name__)
security = HTTPBearer()

router = APIRouter(prefix="/users", tags=["users"])


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str
    full_name: str | None = None
    phone: str | None = None


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class UserResponse(BaseModel):
    id: int
    email: str
    full_name: str | None
    phone: str | None
    is_active: bool

    model_config = {"from_attributes": True}


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


@router.post("/register", response_model=TokenResponse, status_code=201)
def register(body: RegisterRequest, db: Session = Depends(get_db)):
    try:
        logger.info(f"Registering user with email: {body.email}")
        existing = db.query(User).filter(User.email == body.email).first()
        if existing:
            logger.warning(f"Registration failed - email already registered: {body.email}")
            raise HTTPException(status_code=409, detail="Email already registered")

        user = User(
            email=body.email,
            password_hash=hash_password(body.password),
            full_name=body.full_name,
            phone=body.phone,
        )
        db.add(user)
        db.commit()
        db.refresh(user)

        token = create_access_token({"sub": str(user.id)})
        logger.info(f"User registered successfully: {user.id} ({body.email})")
        return TokenResponse(access_token=token, user=UserResponse.model_validate(user))
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error during registration: {e}", exc_info=True)
        raise


@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest, db: Session = Depends(get_db)):
    try:
        logger.info(f"Login attempt for email: {body.email}")
        user = db.query(User).filter(User.email == body.email).first()
        if not user or not verify_password(body.password, user.password_hash):
            logger.warning(f"Login failed - invalid credentials for email: {body.email}")
            raise HTTPException(status_code=401, detail="Invalid credentials")

        token = create_access_token({"sub": str(user.id)})
        logger.info(f"User logged in successfully: {user.id} ({body.email})")
        return TokenResponse(access_token=token, user=UserResponse.model_validate(user))
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error during login: {e}", exc_info=True)
        raise


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db),
) -> User:
    try:
        payload = decode_access_token(credentials.credentials)
        if payload is None:
            logger.warning("Invalid token provided")
            raise HTTPException(status_code=401, detail="Invalid token")
        user_id = int(payload.get("sub"))
        user = db.query(User).filter(User.id == user_id).first()
        if not user:
            logger.warning(f"User not found: {user_id}")
            raise HTTPException(status_code=404, detail="User not found")
        return user
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error validating token: {e}", exc_info=True)
        raise HTTPException(status_code=401, detail="Invalid token")


@router.get("/me", response_model=UserResponse)
def get_profile(current_user: User = Depends(get_current_user)):
    try:
        logger.info(f"Profile retrieved for user: {current_user.id}")
        return UserResponse.model_validate(current_user)
    except Exception as e:
        logger.error(f"Error retrieving profile: {e}", exc_info=True)
        raise


class UpdateProfileRequest(BaseModel):
    full_name: str | None = None
    phone: str | None = None


@router.patch("/me", response_model=UserResponse)
def update_profile(
    body: UpdateProfileRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    try:
        logger.info(f"Updating profile for user: {current_user.id}")
        if body.full_name is not None:
            current_user.full_name = body.full_name
        if body.phone is not None:
            current_user.phone = body.phone
        db.commit()
        db.refresh(current_user)
        logger.info(f"Profile updated successfully for user: {current_user.id}")
        return UserResponse.model_validate(current_user)
    except Exception as e:
        logger.error(f"Error updating profile: {e}", exc_info=True)
        raise
