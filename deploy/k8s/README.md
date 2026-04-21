# Kubernetes Baseline

These manifests are a deployment-readiness baseline for portfolio review.

They intentionally do not replace Docker Compose as the primary local demo. Production Kubernetes deployment would require:

- Real image registry names and tags.
- Kubernetes Secrets or external secret integration for Vault/JWT/database/email values.
- PersistentVolumeClaims for PostgreSQL, RabbitMQ, MinIO, and storage blobs.
- Ingress/TLS configuration.
- Resource requests/limits and horizontal scaling policies.
- Managed database and object storage in a real cloud environment.

Apply order:

```bash
kubectl apply -f namespace.yaml
kubectl apply -f configmaps.yaml
kubectl apply -f auth-service.yaml
kubectl apply -f storage-service.yaml
kubectl apply -f api-gateway.yaml
kubectl apply -f frontend.yaml
```

