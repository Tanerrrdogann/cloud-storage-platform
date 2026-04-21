package com.authsystem.storageservice.entity;

import com.authsystem.storageservice.entity.enums.SagaStatus;
import com.authsystem.storageservice.entity.enums.SagaType;
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
@Table(name = "storage_sagas")
public class StorageSaga {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_type", nullable = false, length = 64)
    private SagaType sagaType;

    @Column(name = "aggregate_id", nullable = false, length = 128)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SagaStatus status;

    @Column(name = "current_step", nullable = false, length = 64)
    private String currentStep;

    @Column(name = "compensation_required", nullable = false)
    private Boolean compensationRequired;

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
        if (status == null) status = SagaStatus.STARTED;
        if (compensationRequired == null) compensationRequired = false;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
