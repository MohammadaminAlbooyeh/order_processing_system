# Quick Start Guide

## Prerequisites
- Docker & Docker Compose installed
- Port 3000, 8001, 8002 available

## Start the Application

### 1. Start all services
```bash
docker-compose up --build
```

This starts:
- PostgreSQL (localhost:5432)
- Redis (localhost:6379)
- Order API (localhost:8001)
- User Service (localhost:8002)
- Frontend (localhost:3000)
- Prometheus (localhost:9090)
- Grafana (localhost:3000)
- Jaeger (localhost:16686)

### 2. Create Admin Test User (in another terminal)
```bash
python seed_admin.py
```

Output:
```
✅ Admin user created successfully!

Login credentials:
  Email: admin@example.com
  Password: Admin@123
```

### 3. Open the App
Visit: **http://localhost:3000**

You should see the Login page.

---

## Login & Test

### Admin Account
```
Email: admin@example.com
Password: Admin@123
```

### After Login, You Can:
1. ✅ View Dashboard (empty, no orders yet)
2. ✅ Click "+ New Order" to create an order
3. ✅ View order details
4. ✅ Update/cancel orders
5. ✅ Filter orders by status
6. ✅ View/update profile

---

## API Documentation

Once services are running:
- **Order API docs**: http://localhost:8001/docs
- **User Service docs**: http://localhost:8002/docs
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Jaeger**: http://localhost:16686

---

## Troubleshooting

### Port 3000 already in use?
Grafana also uses 3000. Either:
- Stop Grafana: `docker-compose stop grafana`
- Or change frontend port in docker-compose.yml

### Database connection error?
Wait for PostgreSQL to be ready (takes ~10 seconds)

### Seed script fails?
Make sure `docker-compose up` finished starting all services first

### Clear everything and restart
```bash
docker-compose down -v  # Remove volumes
docker-compose up --build
python seed_admin.py
```
