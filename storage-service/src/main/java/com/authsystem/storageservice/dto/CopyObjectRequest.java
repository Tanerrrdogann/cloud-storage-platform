package com.authsystem.storageservice.dto;

public record CopyObjectRequest(
        String ownerId,
        String sourceObjectKey,
        String targetObjectKey,
        Boolean overwrite,
        FileConflictPolicy fileConflictPolicy
) {
}
