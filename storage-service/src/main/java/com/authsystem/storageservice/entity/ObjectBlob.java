package com.authsystem.storageservice.entity;

import com.authsystem.storageservice.entity.enums.BlobStatus;
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
@Table(name = "object_blobs")
public class ObjectBlob {

    @Id
    private UUID id;

    @Column(name = "content_hash", nullable = false, unique = true, length = 128)
    private String contentHash;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Column(name = "encrypted_size_bytes", nullable = false)
    private Long encryptedSizeBytes;

    @Column(name = "original_size_bytes", nullable = false)
    private Long originalSizeBytes;

    @Column(name = "encryption_algorithm", nullable = false, length = 64)
    private String encryptionAlgorithm;

    @Column(name = "encryption_iv", nullable = false, length = 64)
    private String encryptionIv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BlobStatus status;

    @Column(name = "reference_count", nullable = false)
    private Long referenceCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (referenceCount == null) referenceCount = 0L;
        if (status == null) status = BlobStatus.ACTIVE;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
