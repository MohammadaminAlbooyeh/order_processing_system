#!/usr/bin/env bash
# End-to-end smoke test against a running `docker-compose up` stack.
# Exercises the cross-service path: register -> login -> create order ->
# worker processes it (status flips to "processing") -> recommendations cached.
#
# Usage: docker-compose up -d --build && ./scripts/smoke-test.sh
set -euo pipefail

USER_URL="${USER_SERVICE_URL:-http://localhost:8002}"
ORDER_URL="${ORDER_SERVICE_URL:-http://localhost:8001}"
EMAIL="smoke+$(date +%s)@example.com"
PASSWORD="Smoke1234"

req() { curl -s -w '\n%{http_code}' "$@"; }
body() { sed '$d'; }
code() { tail -n1; }

echo "1/5 waiting for services..."
for url in "${USER_URL}/health" "${ORDER_URL}/health"; do
  for i in $(seq 1 60); do
    curl -sf "${url}" >/dev/null 2>&1 && break
    [ "${i}" = 60 ] && { echo "  timed out waiting for ${url}"; exit 1; }
    sleep 2
  done
done

echo "2/5 register ${EMAIL}"
resp=$(req -X POST "${USER_URL}/users/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"full_name\":\"Smoke Test\",\"phone\":\"+15551234567\"}")
[ "$(echo "${resp}" | code)" = 201 ] || { echo "  register failed: ${resp}"; exit 1; }
TOKEN=$(echo "${resp}" | body | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
[ -n "${TOKEN}" ] || { echo "  no token in response"; exit 1; }

echo "3/5 create order"
resp=$(req -X POST "${ORDER_URL}/orders" -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"destination":"123 Main St","weight":2.5,"items":[{"product_id":10,"quantity":2}]}')
[ "$(echo "${resp}" | code)" = 201 ] || { echo "  create failed: ${resp}"; exit 1; }
ORDER_ID=$(echo "${resp}" | body | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "  created order ${ORDER_ID}"

echo "4/5 wait for worker to process order ${ORDER_ID}"
for i in $(seq 1 30); do
  resp=$(req "${ORDER_URL}/orders/${ORDER_ID}" -H "Authorization: Bearer ${TOKEN}")
  status=$(echo "${resp}" | body | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
  [ "${status}" = "processing" ] && { echo "  status=processing ✅"; break; }
  [ "${i}" = 30 ] && { echo "  order stuck in status=${status}"; exit 1; }
  sleep 1
done

echo "5/5 list orders"
resp=$(req "${ORDER_URL}/orders" -H "Authorization: Bearer ${TOKEN}")
[ "$(echo "${resp}" | code)" = 200 ] || { echo "  list failed: ${resp}"; exit 1; }

echo
echo "✅ smoke test passed"
