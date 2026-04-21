package com.authsystem.storageservice.dto;

public record CreateFolderRequest(
        String ownerId,
        String folderKey
) {
}
