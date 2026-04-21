package com.authsystem.storageservice.dto;

import java.util.List;

public record ObjectOperationPreviewResponse(
        List<ObjectOperationConflict> fileConflicts,
        List<String> folderMerges,
        String blockedReason
) {
    public boolean hasConflicts() {
        return fileConflicts != null && !fileConflicts.isEmpty();
    }

    public boolean isBlocked() {
        return blockedReason != null && !blockedReason.isBlank();
    }
}
