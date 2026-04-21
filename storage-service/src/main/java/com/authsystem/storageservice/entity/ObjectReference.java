package com.authsystem.storageservice.entity;

import com.authsystem.storageservice.entity.enums.ObjectReferenceStatus;
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
@Table(name = "object_references")
public class ObjectReference {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id", nullable = false)
    private StoredObject object;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blob_id", nullable = false)
    private ObjectBlob blob;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObjectReferenceStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = ObjectReferenceStatus.ACTIVE;
        createdAt = Instant.now();
    }
}
