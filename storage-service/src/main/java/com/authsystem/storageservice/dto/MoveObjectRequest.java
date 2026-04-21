package com.authsystem.storageservice.dto;

public record MoveObjectRequest(
        String ownerId,
        String oldObjectKey,
        String newObjectKey,
        Boolean overwrite,
        FileConflictPolicy fileConflictPolicy
) {
}
