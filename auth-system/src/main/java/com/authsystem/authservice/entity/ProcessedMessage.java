package com.authsystem.authservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "processed_messages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_processed_message_idempotency_key", columnNames = "idempotency_key")
        }
)
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    protected ProcessedMessage() {
    }

    public ProcessedMessage(String idempotencyKey, String eventId, Instant processedAt, String consumerName, String status) {
        this.idempotencyKey = idempotencyKey;
        this.eventId = eventId;
        this.processedAt = processedAt;
        this.consumerName = consumerName;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public String getStatus() {
        return status;
    }

    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.processedAt = Instant.now();
    }

    public void markProcessing() {
        this.status = "PROCESSING";
        this.processedAt = Instant.now();
    }
}
