package com.authsystem.storageservice.dto;

public record ObjectOperationConflict(
        String sourceObjectKey,
        String targetObjectKey,
        String targetType
) {
}
