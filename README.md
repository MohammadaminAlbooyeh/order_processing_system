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

- **Framework:** FastAPI (Python) for the remaining Python services, plus Spring Boot (Java) for the rewritten worker and notification services
- **Database:** PostgreSQL
- **Cache/Queue:** Redis
- **Job Queue:** Redis lists for service-to-service events, plus Celery in the legacy Python worker
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

## Migration Status

The Java rewrite is in progress:

- `services/worker-service-java` replaces the Python worker logic in Java.
- `services/notification-service-java` replaces the Python notification service in Java.
- `services/order-api` and `services/user-service` are still Python and are the next services to port.
- `docker-compose.yml` now points the worker and notification services at the Java Dockerfiles, so those rewrites are active in the default stack.

## Trade-offs & Learning

- Why async? [Explanation]
- Why separate services? [Explanation]
- Database design decisions [Explanation]
- Scaling considerations [Explanation]
