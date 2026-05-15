# E-Commerce Microservices System

## Architecture

This is a production-grade microservices system demonstrating:
- Service separation and communication
- Async job processing with Celery
- PostgreSQL database design
- Redis caching strategy
- Distributed systems concepts

## Services

1. **Order API** - Handles order creation and retrieval
2. **User Service** - User authentication and profiles
3. **Worker Service** - Background job processing
4. **Notification Service** - Email/SMS notifications

## Tech Stack

- **Framework:** FastAPI (Python)
- **Database:** PostgreSQL
- **Cache/Queue:** Redis
- **Job Queue:** Celery
- **Containers:** Docker & Docker Compose
- **Monitoring:** Prometheus, Grafana, Jaeger

## Key Features

- RESTful API design
- JWT authentication
- Async email/SMS notifications
- Distributed tracing
- Metrics monitoring
- Error tracking with Sentry

## Setup

```bash
git clone ...
docker-compose up
# Visit http://localhost:8001/docs for API docs
```

## Trade-offs & Learning

- Why async? [Explanation]
- Why separate services? [Explanation]
- Database design decisions [Explanation]
- Scaling considerations [Explanation]