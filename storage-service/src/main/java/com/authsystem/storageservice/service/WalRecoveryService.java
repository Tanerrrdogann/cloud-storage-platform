package com.authsystem.storageservice.service;

import com.authsystem.storageservice.entity.ObjectBlob;
import com.authsystem.storageservice.entity.StorageWal;
import com.authsystem.storageservice.entity.StoredObject;
import com.authsystem.storageservice.entity.enums.BlobStatus;
import com.authsystem.storageservice.entity.enums.StorageOperationType;
import com.authsystem.storageservice.entity.enums.StoredObjectStatus;
import com.authsystem.storageservice.entity.enums.WalStatus;
import com.authsystem.storageservice.metrics.StorageMetricsService;
import com.authsystem.storageservice.repository.ObjectBlobRepository;
import com.authsystem.storageservice.repository.StorageWalRepository;
import com.authsystem.storageservice.repository.StoredObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalRecoveryService {

    private final StorageWalRepository storageWalRepository;
    private final StoredObjectRepository storedObjectRepository;
    private final ObjectBlobRepository objectBlobRepository;
    private final WalService walService;
    private final StorageMetricsService storageMetricsService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverUnfinishedOperations() {
        List<StorageWal> unfinishedOperations = storageWalRepository.findByStatusIn(List.of(WalStatus.STARTED));

        if (unfinishedOperations.isEmpty()) {
            storageMetricsService.recordWalRecovery("no_unfinished_operations");
            log.info("ST_LOG | ACTION:WAL_RECOVERY_COMPLETED | STATUS:NO_UNFINISHED_OPERATIONS");
            return;
        }

        log.warn(
                "ST_LOG | ACTION:WAL_RECOVERY_STARTED | UNFINISHED_COUNT:{}",
                unfinishedOperations.size()
        );

        unfinishedOperations.forEach(this::recoverSingleOperation);

        log.info(
                "ST_LOG | ACTION:WAL_RECOVERY_COMPLETED | RECOVERED_COUNT:{}",
                unfinishedOperations.size()
        );
        storageMetricsService.recordWalRecovery("completed");
    }

    private void recoverSingleOperation(StorageWal wal) {
        try {
            if (wal.getOperationType() == StorageOperationType.OBJECT_UPLOAD) {
                recoverUpload(wal);
                return;
            }

            if (wal.getOperationType() == StorageOperationType.OBJECT_DELETE) {
                recoverDelete(wal);
                return;
            }

            walService.markFailed(
                    wal.getId(),
                    "Unsupported WAL operation type for recovery: " + wal.getOperationType()
            );
            storageMetricsService.recordWalRecovery("unsupported");

            log.warn(
                    "ST_LOG | ACTION:WAL_RECOVERY_UNSUPPORTED_OPERATION | WAL_ID:{} | OPERATION_TYPE:{}",
                    wal.getId(),
                    wal.getOperationType()
            );
        } catch (Exception ex) {
            walService.markFailed(wal.getId(), "WAL recovery failed: " + ex.getMessage());
            storageMetricsService.recordWalRecovery("failed");

            log.error(
                    "ST_LOG | ACTION:WAL_RECOVERY_FAILED | WAL_ID:{} | OPERATION_TYPE:{} | MSG:{}",
                    wal.getId(),
                    wal.getOperationType(),
                    ex.getMessage()
            );
        }
    }

    private void recoverUpload(StorageWal wal) {
        if (wal.getObjectId() != null && wal.getBlobId() != null) {
            Optional<StoredObject> storedObject = storedObjectRepository.findById(wal.getObjectId());
            Optional<ObjectBlob> blob = objectBlobRepository.findById(wal.getBlobId());

            if (storedObject.isPresent()
                    && blob.isPresent()
                    && storedObject.get().getStatus() == StoredObjectStatus.ACTIVE
                    && blob.get().getStatus() == BlobStatus.ACTIVE) {
                walService.markCompleted(wal.getId(), wal.getObjectId(), wal.getBlobId());

                log.info(
                        "ST_LOG | ACTION:WAL_UPLOAD_RECOVERED_AS_COMPLETED | WAL_ID:{} | OBJECT_ID:{} | BLOB_ID:{}",
                        wal.getId(),
                        wal.getObjectId(),
                        wal.getBlobId()
                );
                storageMetricsService.recordWalRecovery("upload_recovered");
                return;
            }
        }

        walService.markFailed(
                wal.getId(),
                "Unfinished upload detected on startup; metadata transaction was not completed"
        );

        log.warn(
                "ST_LOG | ACTION:WAL_UPLOAD_MARKED_FAILED | WAL_ID:{} | OWNER_ID:{} | OBJECT_KEY:{}",
                wal.getId(),
                wal.getOwnerId(),
                wal.getObjectKey()
        );
        storageMetricsService.recordWalRecovery("upload_marked_failed");
    }

    private void recoverDelete(StorageWal wal) {
        if (wal.getObjectId() == null || wal.getBlobId() == null) {
            walService.markFailed(wal.getId(), "Delete WAL is missing objectId or blobId");

            log.warn(
                    "ST_LOG | ACTION:WAL_DELETE_MARKED_FAILED | WAL_ID:{} | REASON:MISSING_IDS",
                    wal.getId()
            );
            storageMetricsService.recordWalRecovery("delete_marked_failed");
            return;
        }

        Optional<StoredObject> storedObject = storedObjectRepository.findById(wal.getObjectId());
        Optional<ObjectBlob> blob = objectBlobRepository.findById(wal.getBlobId());

        if (storedObject.isPresent()
                && blob.isPresent()
                && storedObject.get().getStatus() == StoredObjectStatus.DELETED
                && (blob.get().getStatus() == BlobStatus.GC_PENDING || blob.get().getStatus() == BlobStatus.ACTIVE)) {
            walService.markCompleted(wal.getId(), wal.getObjectId(), wal.getBlobId());

            log.info(
                    "ST_LOG | ACTION:WAL_DELETE_RECOVERED_AS_COMPLETED | WAL_ID:{} | OBJECT_ID:{} | BLOB_ID:{}",
                    wal.getId(),
                    wal.getObjectId(),
                    wal.getBlobId()
            );
            storageMetricsService.recordWalRecovery("delete_recovered");
            return;
        }

        walService.markFailed(
                wal.getId(),
                "Unfinished delete detected on startup; delete state was not safely completed"
        );

        log.warn(
                "ST_LOG | ACTION:WAL_DELETE_MARKED_FAILED | WAL_ID:{} | OBJECT_ID:{} | BLOB_ID:{}",
                wal.getId(),
                wal.getObjectId(),
                wal.getBlobId()
        );
        storageMetricsService.recordWalRecovery("delete_marked_failed");
    }
}
