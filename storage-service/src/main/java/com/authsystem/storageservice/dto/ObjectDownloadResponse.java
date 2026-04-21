package com.authsystem.storageservice.dto;

public record ObjectDownloadResponse(
        byte[] content,
        String fileName,
        String contentType,
        Long sizeBytes
) {
}
