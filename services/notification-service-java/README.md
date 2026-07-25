# notification-service (Java)

Java/Spring Boot rewrite of the original Python `notification-service`. Same wire
contract as the Python version, so it is a drop-in replacement:

- Polls the Redis list `notification_queue` (same key the Python service and
  `worker-service` use) every 100ms and pops from the left, matching the
  original `lpop`/`lpush` polling loop.
- Handles `ORDER_PROCESSED` events: sends an order confirmation email and an
  SMS with a tracking link.
- Exposes `GET /health` returning `{"status": "healthy"}`.
- Exposes Prometheus metrics at `/actuator/prometheus`.

## Configuration

Same environment variables as the Python service, so it can be dropped into
`docker-compose.yml` without changing the other services:

| Env var | Default | Purpose |
|---|---|---|
| `REDIS_URL` | `redis://localhost:6379` | Redis connection URL |
| `SMTP_SERVER` | `smtp.gmail.com` | SMTP host |
| `SMTP_PORT` | `587` | SMTP port |
| `SMTP_USER` | _(empty)_ | SMTP username / from-address |
| `SMTP_PASSWORD` | _(empty)_ | SMTP password |
| `TWILIO_ACCOUNT_SID` | _(empty)_ | Twilio account SID |
| `TWILIO_AUTH_TOKEN` | _(empty)_ | Twilio auth token |
| `TWILIO_PHONE_NUMBER` | _(empty)_ | Twilio sender number |

If Twilio credentials are not set, SMS sends fall back to a log line
(`[SMS] To: ... - ...`), same as the Python fallback.

## Build & run

Requires Java 17+ and Maven (not installed in this environment yet):

```bash
cd services/notification-service-java
mvn spring-boot:run
```

Build the jar:

```bash
mvn package
java -jar target/notification-service.jar
```

Build the Docker image (build context must be the repo root, same as the
other services' Dockerfiles):

```bash
docker build -f services/notification-service-java/Dockerfile -t notification-service-java .
```

## Not yet wired into docker-compose.yml

This service currently runs standalone. To swap it in for the Python
`notification` service in `docker-compose.yml`, point the `notification`
service's `dockerfile:` at `services/notification-service-java/Dockerfile`
and keep the same environment variables.
