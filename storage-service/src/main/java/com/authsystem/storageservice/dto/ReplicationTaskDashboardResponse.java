package com.authsystem.storageservice.dto;

import java.time.Instant;
import java.util.UUID;

public record ReplicationTaskDashboardResponse(
        UUID id,
        UUID objectId,
        String ownerId,
        String objectKey,
        UUID blobId,
        String target,
        String status,
        Integer retryCount,
        String lastError,
        Instant nextRetryAt,
        Instant updatedAt
) {
}
