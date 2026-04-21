package com.authsystem.authservice.dto;

import java.time.Instant;

public record ProcessedMessageResponse(
        Long id,
        String idempotencyKey,
        String eventId,
        Instant processedAt,
        String consumerName,
        String status
) {
}
