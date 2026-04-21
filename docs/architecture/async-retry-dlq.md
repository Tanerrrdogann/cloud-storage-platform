# Async Retry, DLQ, and Idempotency Flow

```mermaid
flowchart TD
    producer[Producer publishes event] --> exchange[auth.exchange]
    exchange --> queue[Main durable queue]
    queue --> consumer[Manual-ack consumer]
    consumer --> idem{Idempotency key processed?}
    idem -->|Yes| ack1[Ack and skip duplicate]
    idem -->|No| logic[Validate and process]
    logic -->|Success| processed[Mark PROCESSED]
    processed --> ack2[Ack]
    logic -->|Temporary error| retryCheck{Retry count < limit?}
    retryCheck -->|Yes| retryPub[Publish to retry exchange]
    retryPub --> retryQueue[Retry queue with TTL]
    retryQueue --> exchange
    retryCheck -->|No| reject[Reject without requeue]
    logic -->|Permanent error| reject
    reject --> dlx[auth.dlx]
    dlx --> dlq[Dead Letter Queue]
```

Key points:

- Delivery model is at-least-once.
- Duplicate processing is prevented by idempotency keys.
- Temporary failures are retried through TTL retry queues.
- Permanent failures and exhausted retries go to DLQ.

