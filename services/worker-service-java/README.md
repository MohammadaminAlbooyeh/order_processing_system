# worker-service (Java)

Java/Spring Boot rewrite of the original Python `worker-service`.

## Important difference from the Python version

The Python `worker-service` defines Celery tasks (`process_order`,
`calculate_recommendations`) but **nothing in the system dispatches them** —
there is no `.delay()` call anywhere, and `order-api`'s `create_order`
endpoint pushes an `ORDER_CREATED` event onto a plain Redis list,
`order_queue`, that had no consumer at all. The Celery tasks were only ever
invoked directly by the Python test suite.

This Java version closes that gap: **`OrderEventListener` polls `order_queue`
and actually runs `process_order` when it sees an `ORDER_CREATED` event**,
the same way `notification-service` (Java) polls `notification_queue`. This
makes the order → shipping/inventory check → notification pipeline work
end-to-end instead of being dead code.

`calculate_recommendations` is ported as-is: implemented and unit-tested in
`RecommendationService`, but — same as the Python original — nothing
currently triggers it automatically. It's just a callable service method for
now.

## What it does

1. Polls `order_queue` every 100ms (`leftPop`, matching the producer's
   `lpush` in `order-api`).
2. On `ORDER_CREATED`: loads the order from Postgres, calculates shipping,
   checks mock inventory, updates the order status to `PROCESSING`, computes
   product recommendations, and publishes `ORDER_PROCESSED` onto
   `notification_queue` (consumed by `notification-service`).
3. If an item is out of stock, publishes `OUT_OF_STOCK` onto
   `notification_queue` instead and leaves the order status untouched.
4. Exposes `GET /health` and Prometheus metrics at `/actuator/prometheus`.

## Configuration

Same environment variables as the Python worker:

| Env var | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `postgresql://ecommerce:password@localhost:5432/orders` | Postgres connection (parsed into a JDBC URL) |
| `CELERY_BROKER_URL` | `redis://localhost:6379` | Redis connection (kept under the same name as the Python setting, even though this service no longer uses Celery) |

## Build & run

Requires Java 17+ and Maven (not installed in this environment yet):

```bash
cd services/worker-service-java
mvn spring-boot:run
```

Build the jar:

```bash
mvn package
java -jar target/worker-service.jar
```

Build the Docker image (build context must be the repo root):

```bash
docker build -f services/worker-service-java/Dockerfile -t worker-service-java .
```

## Wired into docker-compose.yml

This service is now selected by the default `worker` entry in
`docker-compose.yml`. The Java service listens on port 8000 for `/health`
and `/actuator/prometheus`, so the compose file maps it to host port 8004.

The remaining Python backend services are `order-api` and `user-service`.
