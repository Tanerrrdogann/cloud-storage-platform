package com.authsystem.storageservice.dto;

import java.time.Instant;
import java.util.UUID;

public record StoredObjectDashboardResponse(
        UUID id,
        String ownerId,
        String objectKey,
        String fileName,
        String contentType,
        Long sizeBytes,
        String contentHash,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
