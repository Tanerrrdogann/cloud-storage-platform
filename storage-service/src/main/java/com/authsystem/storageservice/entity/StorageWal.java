package com.authsystem.storageservice.entity;

import com.authsystem.storageservice.entity.enums.StorageOperationType;
import com.authsystem.storageservice.entity.enums.WalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storage_wal")
public class StorageWal {

    @Id
    private UUID id;

    @Column(name = "operation_id", nullable = false, unique = true, length = 128)
    private String operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 64)
    private StorageOperationType operationType;

    @Column(name = "object_id")
    private UUID objectId;

    @Column(name = "blob_id")
    private UUID blobId;

    @Column(name = "owner_id", length = 128)
    private String ownerId;

    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WalStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = WalStatus.STARTED;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
