#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8088/api/v1}"
EMAIL="${DEMO_EMAIL:-demo$(date +%s)@example.com}"
USERNAME="${DEMO_USERNAME:-demo$(date +%s)}"
PASSWORD="${DEMO_PASSWORD:-Demo12345}"

printf '[smoke] Registering %s\n' "$EMAIL"
curl -fsS -X POST "$BASE_URL/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" || true

cat <<MSG

[smoke] Registration request was sent.
If email verification is enabled, verify the user from the database/admin flow before logging in.

Login payload for manual test:
{
  "usernameOrEmail": "$EMAIL",
  "password": "$PASSWORD"
}

Frontend: http://localhost:8088
Gateway through frontend proxy: $BASE_URL
MSG

