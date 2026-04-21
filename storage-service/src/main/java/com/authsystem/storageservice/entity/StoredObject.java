package com.authsystem.storageservice.entity;

import com.authsystem.storageservice.entity.enums.StoredObjectStatus;
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
@Table(name = "stored_objects")
public class StoredObject {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "content_hash", nullable = false, length = 128)
    private String contentHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blob_id", nullable = false)
    private ObjectBlob blob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StoredObjectStatus status;

    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (version == null) version = 1L;
        if (status == null) status = StoredObjectStatus.ACTIVE;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
