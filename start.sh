#!/bin/bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

# Load .env file for plain os.getenv consumers
export $(grep -v '^\s*#' .env | grep -v '^\s*$' | xargs)

# Activate venv
source venv/bin/activate

echo "Starting services..."

PYTHONPATH="$ROOT:$ROOT/services/order-api" \
  nohup uvicorn services.order-api.main:app --host 0.0.0.0 --port 8001 \
  > /tmp/order-api.log 2>&1 &
echo "  Order API     -> http://localhost:8001  (PID $!)"

PYTHONPATH="$ROOT:$ROOT/services/user-service" \
  nohup uvicorn services.user-service.main:app --host 0.0.0.0 --port 8002 \
  > /tmp/user-service.log 2>&1 &
echo "  User Service  -> http://localhost:8002  (PID $!)"

PYTHONPATH="$ROOT:$ROOT/services/notification-service" \
  nohup uvicorn services.notification-service.main:app --host 0.0.0.0 --port 8003 \
  > /tmp/notification-service.log 2>&1 &
echo "  Notification  -> http://localhost:8003  (PID $!)"

PYTHONPATH="$ROOT:$ROOT/services/worker-service" \
  nohup celery -A services.worker-service.tasks.app worker --loglevel=info \
  > /tmp/worker.log 2>&1 &
echo "  Celery Worker -> (PID $!)"

echo ""
echo "Waiting for services..."
sleep 3

for port in 8001 8002 8003; do
  code=$(curl -so /dev/null -w "%{http_code}" http://localhost:$port/health 2>/dev/null || echo "down")
  echo "  port $port: $code"
done

echo ""
echo "Frontend dev server:"
echo "  cd frontend && npm run dev"
echo "  -> http://localhost:5173"
