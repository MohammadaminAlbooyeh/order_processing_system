# Deployment Guide

## Prerequisites

- Docker & Docker Compose
- Java 17+ and Maven (for local development of the backend services)
- Node.js 20+ (for local development of the frontend)
- `curl` and `bash` (for `scripts/seed-admin.sh` and `scripts/smoke-test.sh`)
- PostgreSQL client tools (optional, for debugging)

## Local Development Setup

### 1. Environment Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

Update credentials as needed for development.

### 2. Install Dependencies

Each backend service is an independent Maven project; dependencies are resolved automatically on build:

```bash
cd services/order-api-java && mvn -q dependency:go-offline
cd ../user-service-java && mvn -q dependency:go-offline
cd ../notification-service-java && mvn -q dependency:go-offline
cd ../worker-service-java && mvn -q dependency:go-offline
```

Install frontend dependencies:

```bash
cd frontend && npm install
```

### 3. Database Migrations

Schema migrations are managed by Flyway and run automatically on startup — see [Migration failures](#migration-failures) below for details. No manual migration step is required before starting services.

### 4. Start Services with Docker Compose

```bash
docker-compose up -d --build
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Order API (port 8001)
- User Service (port 8002)
- Notification Service (port 8003)
- Worker Service (port 8004)
- Prometheus (port 9090)
- Grafana (port 3001)
- Jaeger UI (port 16686)
- Frontend (port 3000)

### 5. Health Checks

Monitor service health:

```bash
# Order API health
curl http://localhost:8001/health

# User Service health
curl http://localhost:8002/health

# Notification Service health
curl http://localhost:8003/health

# Worker Service health
curl http://localhost:8004/health

# Prometheus-formatted metrics (any service)
curl http://localhost:8001/actuator/prometheus
```

## Production Deployment

### Database Migrations

Migrations run automatically when `order-api` or `user-service` starts (see [Migration failures](#migration-failures)). No separate migration step is needed as part of a deploy — just make sure the new migration files are included in the image you're rolling out.

### Environment Variables

Set the following environment variables in production:

```
DATABASE_URL=postgresql://user:password@host:5432/dbname
REDIS_URL=redis://redis-host:6379
CELERY_BROKER_URL=redis://redis-host:6379   # Redis URL used by worker-service (name kept from the original Celery-based design)
SECRET_KEY=<strong-random-secret>
LOG_LEVEL=INFO
SMTP_SERVER=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=<your-email>
SMTP_PASSWORD=<your-app-password>
TWILIO_ACCOUNT_SID=<account-sid>
TWILIO_AUTH_TOKEN=<auth-token>
TWILIO_PHONE_NUMBER=<your-number>
```

### Docker Compose Production Build

```bash
docker-compose -f docker-compose.yml up -d
```

### Scaling Considerations

- **Order API**: Stateless, can scale horizontally
- **User Service**: Stateless, can scale horizontally
- **Worker Service**: Can run multiple instances with same Redis broker
- **Notification Service**: Can scale, uses shared Redis queue
- **Database**: Use PostgreSQL replication or managed service (RDS, Cloud SQL)
- **Redis**: Use Redis Cluster or managed service (ElastiCache, Cloud Memorystore)

### Monitoring

- **Prometheus**: Access at http://prometheus-host:9090 — scrapes `/actuator/prometheus` on each service every 15s (config: `monitoring/prometheus.yml`)
- **Grafana**: Access at http://grafana-host:3001 (admin/admin) — the Prometheus datasource and the "Order Processing System" dashboard are auto-provisioned from `monitoring/grafana/provisioning/` and `monitoring/grafana/dashboards/`
- **Jaeger**: Access at http://jaeger-host:16686 for distributed traces — services export via OTLP to `OTLP_ENDPOINT` (defaults to `http://jaeger:4318/v1/traces`)

### Logging

Logs are output to stdout with structured format. Configure log aggregation:

```
LOG_LEVEL=INFO (or DEBUG, WARNING, ERROR)
```

Integrate with ELK Stack, Datadog, New Relic, or similar log aggregators.

## Rollback Procedures

### Database Rollback

Flyway does not support automatic downgrades. If a migration needs to be reverted in production, write a new forward-only migration (e.g. `V3__revert_xyz.sql`) that undoes the change, rather than trying to roll back the schema history. Check the current state first:

```bash
docker-compose exec postgres psql -U ecommerce -d orders -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5"
```

### Service Rollback

Use your deployment platform's rollback mechanism to revert to previous Docker image versions. Since schema changes are forward-only, make sure any migration shipped with a release is backward-compatible with the previous service version until the rollback window closes.

## Troubleshooting

### Services failing to start

Check logs:

```bash
docker-compose logs order-api
docker-compose logs user-service
docker-compose logs postgres
```

### Database connection errors

Verify DATABASE_URL environment variable:

```bash
docker-compose exec postgres psql -U ecommerce -d orders -c "SELECT 1"
```

### Migration failures

Schema migrations are managed by Flyway and run automatically on startup of `order-api` and `user-service` (both point at the same `orders` database, so either one may apply a pending migration first; Flyway's schema history table coordinates this safely). Migration scripts live in each service's `src/main/resources/db/migration/`.

Review applied migrations:

```bash
docker-compose exec postgres psql -U ecommerce -d orders -c "SELECT version, description, success, installed_on FROM flyway_schema_history"
```

To add a new migration, create a new `V{n}__description.sql` file in **both** `services/order-api-java/src/main/resources/db/migration/` and `services/user-service-java/src/main/resources/db/migration/` (kept identical since both services share the database).

## Performance Optimization

- Enable query logging on PostgreSQL for slow query analysis
- Monitor Prometheus metrics for high latency endpoints (see the Grafana "HTTP Request Latency" panel)
- Use Redis caching effectively (Order API caches order reads)
- Monitor the `notification_queue` and `order_queue` Redis list lengths to catch a backed-up worker or notification service
- Set appropriate database connection pool sizes (HikariCP, configured per-service)
