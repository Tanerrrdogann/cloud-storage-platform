package com.authsystem.storageservice.dto;

import java.time.Instant;
import java.util.UUID;

public record StoredObjectResponse(
        UUID objectId,
        String ownerId,
        String objectKey,
        String fileName,
        String contentType,
        Long sizeBytes,
        String contentHash,
        String status,
        Instant createdAt
) {
}
