package com.authsystem.authservice.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseEvent {
    private String eventId;
    private String eventType;
    private Instant occurredAt;
    private String producer;
    private String aggregateId;
    private String idempotencyKey;
    private String correlationId;
    private Integer payloadVersion;
}
