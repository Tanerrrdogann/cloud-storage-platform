# CV / LinkedIn Project Summary

## Short Version

Built a distributed cloud storage and observability platform using Spring Boot, React, RabbitMQ, PostgreSQL, Redis, Vault, MinIO, Prometheus, Grafana, Tempo, and Loki. Implemented gateway-level JWT/RBAC, rate limiting, circuit breakers, idempotent async consumers with retry/DLQ, encrypted object storage, WAL recovery, replication, and centralized telemetry.

## Bullet Version

- Designed and implemented a microservice-based cloud storage platform with API Gateway, Auth Service, Storage Service, React frontend, and observability stack.
- Built RabbitMQ-based asynchronous event processing with durable queues, retry queues, DLQs, manual acknowledgements, and consumer-side idempotency.
- Implemented encrypted object storage with metadata transactions, hash-based deduplication, WAL-style recovery, saga state tracking, garbage collection, and MinIO replication.
- Added Prometheus metrics, Grafana dashboards, Tempo tracing, Loki structured log search, correlation IDs, and Alertmanager rules.
- Packaged the platform with Docker Compose, GitHub Actions CI, k6 load-test scripts, Kubernetes-ready manifests, architecture diagrams, and runtime verification scripts.

## Interview One-Liner

“I built this project to show that I can design backend systems beyond CRUD: secure entrypoints, asynchronous reliability, storage consistency, failure recovery, and observability are all implemented and demonstrable.”

