# Deployment Guide

## Prerequisites

- Docker & Docker Compose
- Python 3.11+ (for local development)
- PostgreSQL client tools (optional, for debugging)

## Local Development Setup

### 1. Environment Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

Update credentials as needed for development.

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

Install service-specific dependencies:

```bash
cd services/order-api && pip install -r requirements.txt
cd ../user-service && pip install -r requirements.txt
cd ../notification-service && pip install -r requirements.txt
cd ../worker-service && pip install -r requirements.txt
```

### 3. Database Migrations

Before starting services, run database migrations:

```bash
# Apply all pending migrations
python migrate.py upgrade

# Check migration status
python migrate.py current

# View migration history
python migrate.py history
```

### 4. Start Services with Docker Compose

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Order API (port 8001)
- User Service (port 8002)
- Notification Service (port 8003)
- Worker Service (background)
- Prometheus (port 9090)
- Grafana (port 3000)
- Jaeger (port 16686)

### 5. Health Checks

Monitor service health:

```bash
# Order API health
curl http://localhost:8001/health

# User Service health
curl http://localhost:8002/health

# Notification Service health
curl http://localhost:8003/health

# Prometheus metrics
curl http://localhost:8001/metrics
```

## Production Deployment

### Database Migrations

Always run migrations before deploying new code:

```bash
python migrate.py upgrade
```

Verify the migration was successful:

```bash
python migrate.py current
```

### Environment Variables

Set the following environment variables in production:

```
DATABASE_URL=postgresql://user:password@host:5432/dbname
REDIS_URL=redis://redis-host:6379
CELERY_BROKER_URL=redis://redis-host:6379
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

- **Prometheus**: Access at http://prometheus-host:9090
- **Grafana**: Access at http://grafana-host:3000 (admin/admin)
- **Jaeger**: Access at http://jaeger-host:16686 for distributed traces

### Logging

Logs are output to stdout with structured format. Configure log aggregation:

```
LOG_LEVEL=INFO (or DEBUG, WARNING, ERROR)
```

Integrate with ELK Stack, Datadog, New Relic, or similar log aggregators.

## Rollback Procedures

### Database Rollback

If a migration fails in production:

```bash
# Check current revision
python migrate.py current

# Rollback to previous revision
python migrate.py downgrade -1
```

### Service Rollback

Use your deployment platform's rollback mechanism to revert to previous Docker image versions.

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
- Monitor Prometheus metrics for high latency endpoints
- Use Redis caching effectively (Order API caches for 1 hour)
- Monitor worker queue depth with Celery Flower
- Set appropriate database connection pool sizes
