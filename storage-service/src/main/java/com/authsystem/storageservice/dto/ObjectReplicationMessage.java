package com.authsystem.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectReplicationMessage {

    private String eventId;
    private String eventType;
    private Instant occurredAt;
    private String producer;
    private String aggregateId;
    private String idempotencyKey;
    private String correlationId;
    private Integer payloadVersion;

    private UUID taskId;
    private UUID objectId;
    private UUID blobId;
    private String ownerId;
    private String objectKey;
    private String storagePath;
    private String target;
}
