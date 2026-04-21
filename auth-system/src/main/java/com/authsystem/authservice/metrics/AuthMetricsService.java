package com.authsystem.authservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordEventPublishSuccess(String eventType) {
        meterRegistry.counter(
                "auth_event_publish_total",
                "event_type", safe(eventType),
                "status", "success"
        ).increment();
    }

    public void recordEventPublishFailure(String eventType) {
        meterRegistry.counter(
                "auth_event_publish_total",
                "event_type", safe(eventType),
                "status", "failure"
        ).increment();
    }

    public void recordConsumerProcessed(String eventType, String consumerName) {
        meterRegistry.counter(
                "auth_consumer_processed_total",
                "event_type", safe(eventType),
                "consumer", safe(consumerName)
        ).increment();
    }

    public void recordConsumerRetry(String eventType, String consumerName) {
        meterRegistry.counter(
                "auth_consumer_retry_total",
                "event_type", safe(eventType),
                "consumer", safe(consumerName)
        ).increment();
    }

    public void recordConsumerDlq(String eventType, String consumerName, String reason) {
        meterRegistry.counter(
                "auth_consumer_dlq_total",
                "event_type", safe(eventType),
                "consumer", safe(consumerName),
                "reason", safe(reason)
        ).increment();
    }

    public void recordDuplicateSkipped(String eventType, String consumerName) {
        meterRegistry.counter(
                "auth_consumer_duplicate_skipped_total",
                "event_type", safe(eventType),
                "consumer", safe(consumerName)
        ).increment();
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}
