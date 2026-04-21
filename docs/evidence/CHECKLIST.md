# Evidence Checklist

Use this checklist before publishing the repository or recording a demo.

## Required Screenshots

Save screenshots into the matching folders under `docs/evidence/screenshots`.

- `prometheus/targets-up.png`: Prometheus `Status > Targets` showing `api-gateway`, `auth-service`, `storage-service`, `rabbitmq`, and `prometheus` as `UP`.
- `grafana/platform-overview.png`: Grafana `Cloud Storage Platform Overview` dashboard.
- `tempo/trace-search.png`: Tempo Explore showing a trace after login or storage activity.
- `loki/correlation-search.png`: Loki Explore query showing logs with `correlationId`.
- `rabbitmq/queues-dlq.png`: RabbitMQ queues showing main queues, retry queues, and DLQs.
- `frontend-user/my-drive.png`: User panel with files/folders.
- `frontend-admin/control-center.png`: Admin panel with users/storage/async observability sections.
- `frontend-moderator/moderation.png`: Moderator panel with limited moderation view.

## Required Text Evidence

- `runtime-output.txt`: paste `./scripts/verify-stack.sh` output from the final local verification run.
- `load-test-results.md`: paste k6 terminal summaries for gateway auth and storage smoke tests.

## Optional Evidence

- `demo-notes.md`: personal notes from a live walkthrough.

## Before Sharing

- Blur real email app passwords, tokens, private keys, and personal data.
- Revoke/rotate any real app password that was ever committed or pushed.
- Confirm no real production secret is committed.
- Confirm local development certificates are generated locally and not treated as production secrets.
- Re-run `./scripts/verify-stack.sh`.
- Confirm frontend opens at `http://localhost:8088`.
- Confirm GitHub Actions CI is green after push.
