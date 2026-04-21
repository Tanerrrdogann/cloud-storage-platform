package com.authsystem.storageservice.dto;

public record ObjectOperationPreviewRequest(
        String ownerId,
        String operationType,
        String sourceObjectKey,
        String targetObjectKey
) {
}
