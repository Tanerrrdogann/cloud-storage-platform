package com.authsystem.storageservice.service;

import com.authsystem.storageservice.entity.StorageWal;
import com.authsystem.storageservice.entity.enums.StorageOperationType;
import com.authsystem.storageservice.entity.enums.WalStatus;
import com.authsystem.storageservice.repository.StorageWalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalService {

    private final StorageWalRepository storageWalRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StorageWal startUpload(String operationId, String ownerId, String objectKey) {
        StorageWal wal = StorageWal.builder()
                .operationId(operationId)
                .operationType(StorageOperationType.OBJECT_UPLOAD)
                .ownerId(ownerId)
                .objectKey(objectKey)
                .status(WalStatus.STARTED)
                .build();

        StorageWal savedWal = storageWalRepository.save(wal);

        log.info(
                "ST_LOG | ACTION:STORAGE_WAL_STARTED | OPERATION_ID:{} | OWNER_ID:{} | OBJECT_KEY:{}",
                operationId,
                ownerId,
                objectKey
        );

        return savedWal;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StorageWal startDelete(String ownerId, String objectKey, UUID objectId, UUID blobId) {
        String operationId = UUID.randomUUID().toString();

        StorageWal wal = StorageWal.builder()
                .operationId(operationId)
                .operationType(StorageOperationType.OBJECT_DELETE)
                .ownerId(ownerId)
                .objectKey(objectKey)
                .objectId(objectId)
                .blobId(blobId)
                .status(WalStatus.STARTED)
                .build();

        StorageWal savedWal = storageWalRepository.save(wal);

        log.info(
                "ST_LOG | ACTION:STORAGE_DELETE_WAL_STARTED | OPERATION_ID:{} | OWNER_ID:{} | OBJECT_KEY:{} | OBJECT_ID:{} | BLOB_ID:{}",
                operationId,
                ownerId,
                objectKey,
                objectId,
                blobId
        );

        return savedWal;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID walId, UUID objectId, UUID blobId) {
        StorageWal wal = storageWalRepository.findById(walId)
                .orElseThrow(() -> new IllegalArgumentException("WAL record not found"));

        wal.setObjectId(objectId);
        wal.setBlobId(blobId);
        wal.setStatus(WalStatus.COMPLETED);
        storageWalRepository.save(wal);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID walId, String errorMessage) {
        StorageWal wal = storageWalRepository.findById(walId)
                .orElseThrow(() -> new IllegalArgumentException("WAL record not found"));

        wal.setStatus(WalStatus.FAILED);
        wal.setErrorMessage(errorMessage);
        storageWalRepository.save(wal);
    }
}
