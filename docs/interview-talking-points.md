# Interview Talking Points

## Why RabbitMQ Retry Queues?

Queue-based retry keeps retry behavior inside the distributed messaging system instead of only inside one process. A failed service restart does not erase retry intent, and DLQ visibility is operationally clear.

## Why Idempotency?

RabbitMQ delivery is at-least-once, so duplicate messages are possible. Consumer-side idempotency prevents duplicate business effects when a message is redelivered.

## Why WAL in Storage?

Object storage has multiple steps: write encrypted bytes, store metadata, update references, and publish replication tasks. WAL-style records make startup recovery and incomplete operation detection possible.

## Why Saga State?

Storage operations can cross local disk, database metadata, RabbitMQ, and MinIO replication. Saga state makes multi-step progress and compensation visible.

## Why Prometheus, Grafana, Tempo, Loki?

Metrics show system health and business activity, traces show request paths across services, logs explain what happened, and correlation IDs connect all three during debugging.

## Tradeoffs

- Docker Compose is the primary local demo because it is reproducible on one machine.
- Kubernetes manifests are included as readiness artefacts, not as the primary demo runtime.
- Fixed TTL retry is used first; exponential retry queue chains can be added later.
- Folder behavior uses object key prefixes instead of a separate folder table to keep object-storage semantics simple.

