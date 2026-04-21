# Runtime Verification

Run these commands from the repository root.

## Compile and Build

```bash
cd auth-system && mvn -q -DskipTests compile
cd ../api-gateway && mvn -q -DskipTests compile
cd ../storage-service && mvn -q -DskipTests compile
cd ../frontend && npm install && npm run build
```

Expected result: all commands exit successfully.

## Docker Compose

```bash
cd auth-system
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Expected result:

- `api-gateway`, `auth-service`, `storage-service`, `frontend`, databases, RabbitMQ, Redis, Vault, MinIO, Prometheus, Grafana, Tempo, Loki, Promtail, Alertmanager are running.
- Frontend is reachable at `http://localhost:8088`.

## Prometheus Targets

Open `http://localhost:9090/targets`.

Expected `UP` targets:

- `api-gateway:9002`
- `auth-service:9001`
- `storage-service:9003`
- `rabbitmq:15692`
- `prometheus:9090`

## RabbitMQ Queues

```bash
cd auth-system
docker compose exec -T rabbitmq rabbitmqctl list_queues name messages arguments
```

Expected result: auth and storage main/retry/DLQ queues exist. Healthy demo state normally has `0` messages.

## Grafana Datasources

```bash
curl -s -u admin:admin http://localhost:3000/api/datasources
```

Expected result: `Prometheus`, `Tempo`, and `Loki` datasources exist.

## Frontend Proxy Smoke Test

```bash
curl -i -s -X POST http://localhost:8088/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"none@example.com","password":"bad"}'
```

Expected result: `401 Unauthorized` from the auth service. This proves frontend Nginx proxies `/api/v1/**` to the gateway.
