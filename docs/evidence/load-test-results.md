# Load Test Results

Status: pending final local run.

Paste k6 command output here after running:

```bash
k6 run load-tests/k6/gateway-auth-smoke.js
k6 run load-tests/k6/storage-smoke.js
```

Suggested evidence:

- Requests per second
- p95 latency
- HTTP failure rate
- Notes about machine specs and Docker resource limits

## Gateway Auth Smoke

Command:

```bash
k6 run load-tests/k6/gateway-auth-smoke.js
```

Paste summary:

```text
pending
```

## Storage Smoke

Command:

```bash
k6 run load-tests/k6/storage-smoke.js
```

Paste summary:

```text
pending
```

## Notes

- Run against the same local Docker Compose stack used for screenshots.
- Record Docker CPU/memory limits if they are configured.
