# E-Commerce Microservices System

## Architecture

This is a production-grade microservices system demonstrating:

- Service separation and communication
- Async job processing via Redis-backed event queues
- PostgreSQL database design
- Redis caching strategy
- Distributed systems concepts (tracing, metrics, health checks)

## Services

All backend services are Java/Spring Boot applications running on port `8000`.

| Service | Container name | Port (host) | Responsibility |
|---------|----------------|-------------|----------------|
| Order API | `order-api` | 8001 | Order creation, retrieval, caching, JWT auth |
| User Service | `user-service` | 8002 | Registration, login, JWT issuance, profile management |
| Notification Service | `notification` | 8003 | Email/SMS notifications via Redis events |
| Worker Service | `worker` | 8004 | Background processing of order events |

Supporting infrastructure: PostgreSQL, Redis, Prometheus, Grafana, Jaeger, and a React frontend (`frontend`).

## Tech Stack

- **Backend framework:** Spring Boot 3.5 (Java 17), Spring Security, Spring Data JPA
- **Frontend:** React
- **Database:** PostgreSQL 15
- **Cache / Message queue:** Redis 7
- **Authentication:** JWT (jjwt 0.12)
- **Containers:** Docker & Docker Compose
- **Monitoring:** Prometheus, Grafana
- **Distributed tracing:** Jaeger
- **Metrics:** Micrometer + Prometheus registry, Spring Boot Actuator

## Key Features

- RESTful API design
- JWT authentication and authorization
- Async email/SMS notifications through Redis event listeners
- Distributed tracing with Jaeger
- Metrics monitoring via Prometheus/Grafana
- Health checks and Actuator endpoints
- Caching of order reads in Redis

## Setup

```bash
docker-compose up --build
```

Host ports:

- Order API: http://localhost:8001
- User Service: http://localhost:8002
- Notification Service: http://localhost:8003
- Worker Service: http://localhost:8004
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin / admin)
- Jaeger UI: http://localhost:16686
- Frontend: http://localhost:3000

### Seeding an admin user

```bash
./scripts/seed-admin.sh
```

Creates `admin@example.com` / `Admin@123` via the user-service register endpoint
(uses `curl` only — no Python required).

### End-to-end smoke test

```bash
docker-compose up -d --build
./scripts/smoke-test.sh
```

Registers a user, creates an order, and asserts the worker moves it to
`processing` — a quick check that the cross-service Redis event path works.

## Service Communication

- Order API publishes order-created events to Redis on order creation.
- Worker and Notification services listen on those Redis lists/streams and process them asynchronously.
- Order API caches order responses in Redis to reduce database load.

## Monitoring

- Each service exposes `management.endpoints.web.exposure.include=health,prometheus`.
- Metrics are scraped from `/actuator/prometheus` (Prometheus job targets in `monitoring/prometheus.yml`).
- Traces are exported to Jaeger via OpenTelemetry.

## Architecture Notes

- `docker-compose.yml` wires all four backend services to their Java Dockerfiles.
- Each service builds from `services/<name>-java/Dockerfile` via Maven.
- Environment variables (e.g. `DATABASE_URL`, `REDIS_URL`, `SECRET_KEY`) configure connection details.

## Trade-offs & Decisions

- **Why async?** Order creation and notifications are decoupled through Redis so slow operations (email/SMS) never block the API request path, improving latency and resilience.
- **Why separate services?** Each service can be scaled, deployed, and failure-isolated independently; the codebase maps cleanly to team/ownership boundaries.
- **Database design decisions:** Orders and users live in a shared PostgreSQL database with separate access layers per service; order items are stored as JSONB and parsed on read, keeping a single-row-per-order model simple.
- **Scaling considerations:** Services are horizontally scalable behind a load balancer. Redis serves as both cache and message queue; for high throughput, swap Redis streams for a dedicated broker (e.g. Kafka) and add connection pooling.
- **Why Java/Spring Boot?** Replaced the original FastAPI/Celery services for stronger typing, a mature ecosystem, and built-in Actuator/micrometer observability.