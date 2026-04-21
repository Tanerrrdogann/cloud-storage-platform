package com.authsystem.storageservice.service;

import com.authsystem.storageservice.entity.ObjectBlob;
import com.authsystem.storageservice.entity.enums.BlobStatus;
import com.authsystem.storageservice.entity.enums.ObjectReferenceStatus;
import com.authsystem.storageservice.metrics.StorageMetricsService;
import com.authsystem.storageservice.repository.ObjectBlobRepository;
import com.authsystem.storageservice.repository.ObjectReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarbageCollectionWorker {

    private final ObjectBlobRepository objectBlobRepository;
    private final ObjectReferenceRepository objectReferenceRepository;
    private final StorageMetricsService storageMetricsService;

    @Scheduled(fixedDelayString = "${storage.gc.fixed-delay-ms:60000}")
    @Transactional
    public void cleanGcPendingBlobs() {
        List<ObjectBlob> gcPendingBlobs = objectBlobRepository.findByStatus(BlobStatus.GC_PENDING);

        if (gcPendingBlobs.isEmpty()) {
            log.debug("ST_LOG | ACTION:GC_SKIPPED | REASON:NO_GC_PENDING_BLOBS");
            return;
        }

        log.info(
                "ST_LOG | ACTION:GC_STARTED | CANDIDATE_COUNT:{}",
                gcPendingBlobs.size()
        );

        for (ObjectBlob blob : gcPendingBlobs) {
            cleanSingleBlob(blob);
        }

        log.info(
                "ST_LOG | ACTION:GC_COMPLETED | CANDIDATE_COUNT:{}",
                gcPendingBlobs.size()
        );
    }

    private void cleanSingleBlob(ObjectBlob blob) {
        long activeReferenceCount = objectReferenceRepository.countByBlob_IdAndStatus(
                blob.getId(),
                ObjectReferenceStatus.ACTIVE
        );

        if (activeReferenceCount > 0) {
            log.warn(
                    "ST_LOG | ACTION:GC_BLOB_SKIPPED | REASON:ACTIVE_REFERENCES_EXIST | BLOB_ID:{} | ACTIVE_REFERENCES:{}",
                    blob.getId(),
                    activeReferenceCount
            );
            return;
        }

        try {
            Path blobPath = Path.of(blob.getStoragePath());

            if (Files.exists(blobPath)) {
                Files.delete(blobPath);
            }

            blob.setStatus(BlobStatus.DELETED);
            objectBlobRepository.save(blob);
            storageMetricsService.recordGarbageCollectedBlob();

            log.info(
                    "ST_LOG | ACTION:GC_BLOB_DELETED | BLOB_ID:{} | PATH:{}",
                    blob.getId(),
                    blobPath
            );
        } catch (Exception ex) {
            log.error(
                    "ST_LOG | ACTION:GC_BLOB_DELETE_FAILED | BLOB_ID:{} | PATH:{} | MSG:{}",
                    blob.getId(),
                    blob.getStoragePath(),
                    ex.getMessage()
            );
        }
    }
}
