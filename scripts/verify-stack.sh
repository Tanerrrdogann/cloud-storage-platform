#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

info() {
  printf '\n[verify] %s\n' "$1"
}

info "Compiling auth-system"
(cd "$ROOT_DIR/auth-system" && mvn -q -DskipTests compile)

info "Compiling api-gateway"
(cd "$ROOT_DIR/api-gateway" && mvn -q -DskipTests compile)

info "Compiling storage-service"
(cd "$ROOT_DIR/storage-service" && mvn -q -DskipTests compile)

info "Building frontend"
(cd "$ROOT_DIR/frontend" && npm install && npm run build)

info "Validating Docker Compose"
(cd "$ROOT_DIR/auth-system" && docker compose config --quiet)

info "Checking Docker Compose service status"
(cd "$ROOT_DIR/auth-system" && docker compose ps)

info "Checking frontend endpoint"
curl -fsS http://localhost:8088 >/dev/null

info "Checking frontend proxy to gateway/auth"
curl -sS -o /tmp/cloud-storage-platform-login-check.json -w "%{http_code}" \
  -X POST http://localhost:8088/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"none@example.com","password":"bad"}' | grep -Eq '401|429'

info "Checking Prometheus targets"
curl -fsS http://localhost:9090/api/v1/targets | grep -q '"health":"up"'

info "Checking Grafana datasources"
curl -fsS -u admin:admin http://localhost:3000/api/datasources | grep -Eq 'Prometheus|Tempo|Loki'

info "Checking RabbitMQ queues"
(cd "$ROOT_DIR/auth-system" && docker compose exec -T rabbitmq rabbitmqctl list_queues name messages arguments) > /tmp/cloud-storage-platform-rabbitmq-queues.txt
grep -Eq 'dlq|retry.queue' /tmp/cloud-storage-platform-rabbitmq-queues.txt

info "Stack verification completed"
