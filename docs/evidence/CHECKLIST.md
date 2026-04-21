# Evidence Checklist

Use this checklist before publishing the repository or recording a demo.

## Required Screenshots

Save screenshots into the matching folders under `docs/evidence/screenshots`.

### Frontend Auth Flow

- `frontend-user/login.png`: Sign in page at `http://localhost:8088`.
- `frontend-user/register.png`: Create account form.
- `frontend-user/register-success.png`: Registration success state after creating a demo account.
- `frontend-user/forgot-password.png`: Forgot password form.
- `frontend-user/forgot-password-sent.png`: Reset email sent state.
- `frontend-user/reset-password.png`: Reset password form with a token URL.
- `frontend-user/reset-password-missing-token.png`: Reset password page showing missing-token validation.
- `frontend-user/verify-email-loading.png`: Verify email page while processing a token.
- `frontend-user/verify-email-success.png`: Account verified state.
- `frontend-user/verify-email-failed.png`: Invalid or expired verification token state.

### Frontend User Drive

- `frontend-user/my-drive-empty.png`: User panel before uploads, showing empty-state and upload actions.
- `frontend-user/my-drive.png`: User panel with multiple files and folders.
- `frontend-user/upload-success.png`: Drive after a successful upload with a success toast.
- `frontend-user/folder-created.png`: Folder visible in My Drive.
- `frontend-user/folder-open.png`: Inside a folder with breadcrumb navigation.
- `frontend-user/context-menu-file.png`: Right-click file actions: copy, cut, rename/move, delete, download.
- `frontend-user/context-menu-folder.png`: Right-click folder actions: open, copy, cut, paste, rename, move, delete.
- `frontend-user/move-modal.png`: Move dialog with available folder targets and custom path.
- `frontend-user/conflict-dialog.png`: File conflict dialog showing replace or keep-both options.
- `frontend-user/delete-folder-confirm.png`: Recursive folder delete confirmation.
- `frontend-user/recent-uploads.png`: Recent uploads section showing encrypted object keys.

### Frontend Admin

- `frontend-admin/control-center.png`: Admin landing view with platform stats and operational insights.
- `frontend-admin/users.png`: User management tab showing RBAC, suspend, unsuspend, delete actions.
- `frontend-admin/storage-inventory.png`: Storage object inventory with object key, owner, hash, and status.
- `frontend-admin/async-engine.png`: Async processed-message and consumer distribution tables.
- `frontend-admin/replication.png`: Replication tasks with retry count and error visibility.
- `frontend-admin/wal-saga.png`: WAL entries and saga state side by side.
- `frontend-admin/observability-tab.png`: Admin observability tab with Grafana link.

### Frontend Moderator

- `frontend-moderator/moderation.png`: Moderator console with limited user moderation actions.
- `frontend-moderator/recent-activity.png`: Recent activity table.
- `frontend-moderator/grafana-link.png`: Moderator panel showing observability access.

### Observability and Infrastructure

- `prometheus/targets-up.png`: Prometheus `Status > Targets` showing `api-gateway`, `auth-service`, `storage-service`, `rabbitmq`, and `prometheus` as `UP`.
- `prometheus/rules-alerts.png`: Prometheus alerts/rules page showing configured platform alerts.
- `grafana/platform-overview.png`: Grafana `Cloud Storage Platform Overview` dashboard.
- `grafana/request-latency.png`: Grafana panel focused on API latency and request rate.
- `grafana/error-rate.png`: Grafana panel focused on error rate.
- `grafana/rabbitmq-queues.png`: Grafana panel showing RabbitMQ queue depth.
- `tempo/trace-search.png`: Tempo Explore showing a trace after login or storage activity.
- `tempo/trace-detail.png`: Tempo trace detail with gateway/auth/storage span hierarchy.
- `loki/correlation-search.png`: Loki Explore query showing logs with `correlationId`.
- `loki/upload-log.png`: Loki log result for an upload request with method, path, status, and correlation ID.
- `rabbitmq/queues-dlq.png`: RabbitMQ queues showing main queues, retry queues, and DLQs.
- `rabbitmq/exchanges-bindings.png`: RabbitMQ exchanges/bindings showing auth retry/DLQ topology.
- `rabbitmq/consumers.png`: RabbitMQ queue detail showing active consumers and acknowledgements.
- `minio/bucket-objects.png`: MinIO bucket view showing replicated object artifacts.

### Local Runtime Proof

- `runtime/docker-compose-ps.png`: Terminal or IDE screenshot of `docker compose ps` showing main containers running.
- `swagger/auth-openapi.png`: Auth service Swagger/OpenAPI page.
- `swagger/storage-openapi.png`: Storage service Swagger/OpenAPI page.

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
