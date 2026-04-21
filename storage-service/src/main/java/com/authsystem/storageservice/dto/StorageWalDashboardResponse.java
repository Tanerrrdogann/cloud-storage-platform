package com.authsystem.storageservice.dto;

import java.time.Instant;
import java.util.UUID;

public record StorageWalDashboardResponse(
        UUID id,
        String operationId,
        String operationType,
        UUID objectId,
        UUID blobId,
        String ownerId,
        String objectKey,
        String status,
        String errorMessage,
        Instant updatedAt
) {
}
