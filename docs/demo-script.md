# Demo Script

Target length: 5-8 minutes.

## 1. Open with the Problem

“This is a distributed cloud storage and observability platform. I built it to demonstrate backend system design: central gateway security, asynchronous processing, idempotent consumers, encrypted object storage, recovery, replication, and full telemetry.”

## 2. Show the Running System

- Open `http://localhost:8088`.
- Mention that the frontend is served by Nginx and proxies API traffic through the gateway.
- Open `docker compose ps` or Prometheus targets to show all services are running.

## 3. User Flow

- Log in or register a user.
- Upload a file.
- Create a folder.
- Move/copy/delete a file.
- Explain that folders are implemented through object key prefixes and marker objects.

## 4. Admin Flow

- Open the admin panel.
- Show users, storage objects, async summary, replication tasks, WAL, and saga state.
- Explain role-based visibility.

## 5. Async Engine

- Open RabbitMQ.
- Show main queues, retry queues, and DLQs.
- Explain at-least-once delivery, idempotency keys, retry limits, and DLQ behavior.

## 6. Storage Engine

- Explain encryption at rest, hash-based deduplication, WAL recovery, saga tracking, MinIO replication, and GC.

## 7. Observability

- Open Grafana.
- Show the platform overview dashboard.
- Open Prometheus targets.
- Show Loki correlation search and Tempo traces if available.

## 8. Close

“The goal was not only to make CRUD endpoints, but to build something closer to a real distributed backend: failure handling, visibility, and operational proof are part of the design.”

