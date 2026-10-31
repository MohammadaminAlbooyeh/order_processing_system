#!/usr/bin/env bash
# Seed an admin test user by calling the user-service REST API.
# Replaces the old Python seed_admin.py so the repo has no Python runtime dependency.
set -euo pipefail

BASE_URL="${USER_SERVICE_URL:-http://localhost:8002}"
EMAIL="admin@example.com"
PASSWORD="Admin@123"

payload=$(cat <<JSON
{"email":"${EMAIL}","password":"${PASSWORD}","full_name":"Admin User","phone":"+1234567890"}
JSON
)

status=$(curl -s -o /tmp/seed-admin-resp.json -w '%{http_code}' \
  -X POST "${BASE_URL}/users/register" \
  -H 'Content-Type: application/json' \
  -d "${payload}")

case "${status}" in
  201) echo "✅ Admin user created." ;;
  409) echo "✅ Admin user already exists." ;;
  *)   echo "❌ Failed (HTTP ${status}): $(cat /tmp/seed-admin-resp.json)"; exit 1 ;;
esac

echo
echo "Login credentials:"
echo "  Email:    ${EMAIL}"
echo "  Password: ${PASSWORD}"
