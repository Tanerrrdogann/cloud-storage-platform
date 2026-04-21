package com.authsystem.storageservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class StorageMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordUploadSuccess(Duration duration) {
        meterRegistry.counter("storage_object_upload_total", "status", "success").increment();
        recordUploadDuration(duration);
    }

    public void recordUploadFailure(Duration duration) {
        meterRegistry.counter("storage_object_upload_total", "status", "failure").increment();
        recordUploadDuration(duration);
    }

    public void recordDownloadSuccess(Duration duration) {
        meterRegistry.counter("storage_object_download_total", "status", "success").increment();
        recordDownloadDuration(duration);
    }

    public void recordDownloadFailure(Duration duration) {
        meterRegistry.counter("storage_object_download_total", "status", "failure").increment();
        recordDownloadDuration(duration);
    }

    public void recordDeleteSuccess() {
        meterRegistry.counter("storage_object_delete_total", "status", "success").increment();
    }

    public void recordDeleteFailure() {
        meterRegistry.counter("storage_object_delete_total", "status", "failure").increment();
    }

    public void recordReplicationSuccess() {
        meterRegistry.counter("storage_replication_total", "status", "success").increment();
    }

    public void recordReplicationFailure(String reason) {
        meterRegistry.counter(
                "storage_replication_total",
                "status", "failure",
                "reason", safe(reason)
        ).increment();
    }

    public void recordWalRecovery(String status) {
        meterRegistry.counter(
                "storage_wal_recovery_total",
                "status", safe(status)
        ).increment();
    }

    public void recordGarbageCollectedBlob() {
        meterRegistry.counter("storage_gc_blob_deleted_total").increment();
    }

    private void recordUploadDuration(Duration duration) {
        Timer.builder("storage_object_upload_duration")
                .description("Object upload duration")
                .register(meterRegistry)
                .record(duration);
    }

    private void recordDownloadDuration(Duration duration) {
        Timer.builder("storage_object_download_duration")
                .description("Object download duration")
                .register(meterRegistry)
                .record(duration);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}
