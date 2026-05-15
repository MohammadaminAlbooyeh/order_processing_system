from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from shared.database import Base
import os

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://ecommerce:password@postgres:5432/orders")

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
