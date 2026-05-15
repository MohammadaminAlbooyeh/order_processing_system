from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from pydantic import BaseModel, EmailStr, Field, field_validator
from database import get_db
from models import User
from auth import hash_password, verify_password, create_access_token, decode_access_token
import sys
import os
import re

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../'))
from shared.logging_config import setup_logging

logger = setup_logging(__name__)
security = HTTPBearer()

router = APIRouter(prefix="/users", tags=["users"])


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8, max_length=100, description="Password must be 8-100 characters")
    full_name: str | None = Field(None, min_length=2, max_length=100)
    phone: str | None = Field(None, max_length=20)

    @field_validator("password")
    @classmethod
    def validate_password(cls, v: str) -> str:
        if not any(c.isupper() for c in v):
            raise ValueError("Password must contain at least one uppercase letter")
        if not any(c.isdigit() for c in v):
            raise ValueError("Password must contain at least one digit")
        return v

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v: str | None) -> str | None:
        if v is None:
            return v
        if not re.match(r"^\+?1?\d{9,15}$", v):
            raise ValueError("Phone number must be a valid format (9-15 digits, optional + prefix)")
        return v

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str | None) -> str | None:
        if v is not None and not re.match(r"^[a-zA-Z\s'-]{2,100}$", v):
            raise ValueError("Full name can only contain letters, spaces, hyphens, and apostrophes")
        return v


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


@router.get("/{user_id}", response_model=UserResponse)
def get_user(user_id: int, db: Session = Depends(get_db)):
    try:
        logger.info(f"Fetching user: {user_id}")
        user = db.query(User).filter(User.id == user_id).first()
        if not user:
            logger.warning(f"User not found: {user_id}")
            raise HTTPException(status_code=404, detail="User not found")
        logger.info(f"User {user_id} retrieved successfully")
        return UserResponse.model_validate(user)
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error retrieving user {user_id}: {e}", exc_info=True)
        raise


@router.get("/me", response_model=UserResponse)
def get_profile(current_user: User = Depends(get_current_user)):
    try:
        logger.info(f"Profile retrieved for user: {current_user.id}")
        return UserResponse.model_validate(current_user)
    except Exception as e:
        logger.error(f"Error retrieving profile: {e}", exc_info=True)
        raise


class UpdateProfileRequest(BaseModel):
    full_name: str | None = Field(None, min_length=2, max_length=100)
    phone: str | None = Field(None, max_length=20)

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v: str | None) -> str | None:
        if v is None:
            return v
        if not re.match(r"^\+?1?\d{9,15}$", v):
            raise ValueError("Phone number must be a valid format (9-15 digits, optional + prefix)")
        return v

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str | None) -> str | None:
        if v is not None and not re.match(r"^[a-zA-Z\s'-]{2,100}$", v):
            raise ValueError("Full name can only contain letters, spaces, hyphens, and apostrophes")
        return v


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
