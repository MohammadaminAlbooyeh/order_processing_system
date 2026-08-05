#!/usr/bin/env python3
"""Seed database with admin test user via HTTP API"""
import httpx

ADMIN_EMAIL = "admin@example.com"
ADMIN_PASSWORD = "Admin@123"
ADMIN_FULL_NAME = "Admin User"
ADMIN_PHONE = "+1234567890"
BASE_URL = "http://localhost:8002"

def seed_admin():
    payload = {
        "email": ADMIN_EMAIL,
        "password": ADMIN_PASSWORD,
        "full_name": ADMIN_FULL_NAME,
        "phone": ADMIN_PHONE,
    }
    with httpx.Client() as client:
        response = client.post(f"{BASE_URL}/users/register", json=payload, timeout=10.0)

    if response.status_code == 201:
        data = response.json()
        print("✅ Admin user created successfully!")
        print(f"\nLogin credentials:")
        print(f"  Email: {ADMIN_EMAIL}")
        print(f"  Password: {ADMIN_PASSWORD}")
        print(f"\nUser ID: {data.get('user', {}).get('id', 'N/A')}")
    elif response.status_code == 409:
        print("✅ Admin user already exists!")
        print(f"\nLogin credentials:")
        print(f"  Email: {ADMIN_EMAIL}")
        print(f"  Password: {ADMIN_PASSWORD}")
    else:
        print(f"❌ Error creating admin user: {response.status_code} - {response.text}")

if __name__ == "__main__":
    seed_admin()
