package com.authsystem.storageservice.dto;

import java.time.Instant;
import java.util.UUID;

public record StorageSagaDashboardResponse(
        UUID id,
        String sagaType,
        String aggregateId,
        String status,
        String currentStep,
        Boolean compensationRequired,
        String errorMessage,
        Instant updatedAt
) {
}
