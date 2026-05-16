#!/usr/bin/env python3
"""Seed database with admin test user"""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'services/user-service'))

from database import SessionLocal
from models import User
from auth import hash_password

def seed_admin():
    db = SessionLocal()
    try:
        # Check if admin already exists
        existing = db.query(User).filter(User.email == "admin@example.com").first()
        if existing:
            print("✅ Admin user already exists!")
            print(f"   Email: {existing.email}")
            print(f"   Password: Admin@123")
            return

        # Create admin user
        admin = User(
            email="admin@example.com",
            password_hash=hash_password("Admin@123"),
            full_name="Admin User",
            phone="+1234567890",
            is_active=True,
        )
        db.add(admin)
        db.commit()
        db.refresh(admin)

        print("✅ Admin user created successfully!")
        print(f"\nLogin credentials:")
        print(f"  Email: admin@example.com")
        print(f"  Password: Admin@123")
        print(f"\nUser ID: {admin.id}")

    except Exception as e:
        db.rollback()
        print(f"❌ Error creating admin user: {e}")
    finally:
        db.close()

if __name__ == "__main__":
    seed_admin()
