package com.authsystem.storageservice.service;

import com.authsystem.storageservice.dto.FileConflictPolicy;
import com.authsystem.storageservice.dto.ObjectOperationConflict;
import com.authsystem.storageservice.dto.ObjectOperationPreviewResponse;
import com.authsystem.storageservice.dto.StoredObjectResponse;
import com.authsystem.storageservice.dto.ObjectDownloadResponse;
import com.authsystem.storageservice.dto.ReplicationTaskDashboardResponse;
import com.authsystem.storageservice.dto.StorageSagaDashboardResponse;
import com.authsystem.storageservice.dto.StorageWalDashboardResponse;
import com.authsystem.storageservice.dto.StoredObjectDashboardResponse;
import com.authsystem.storageservice.entity.ObjectBlob;
import com.authsystem.storageservice.entity.ObjectReference;
import com.authsystem.storageservice.entity.ReplicationTask;
import com.authsystem.storageservice.entity.StorageSaga;
import com.authsystem.storageservice.entity.StorageWal;
import com.authsystem.storageservice.entity.StoredObject;
import com.authsystem.storageservice.entity.enums.BlobStatus;
import com.authsystem.storageservice.entity.enums.ObjectReferenceStatus;
import com.authsystem.storageservice.entity.enums.SagaType;
import com.authsystem.storageservice.entity.enums.StorageOperationType;
import com.authsystem.storageservice.entity.enums.StoredObjectStatus;
import com.authsystem.storageservice.entity.enums.WalStatus;
import com.authsystem.storageservice.exception.ObjectOperationConflictException;
import com.authsystem.storageservice.metrics.StorageMetricsService;
import com.authsystem.storageservice.repository.ObjectBlobRepository;
import com.authsystem.storageservice.repository.ObjectReferenceRepository;
import com.authsystem.storageservice.repository.ReplicationTaskRepository;
import com.authsystem.storageservice.repository.StorageSagaRepository;
import com.authsystem.storageservice.repository.StorageWalRepository;
import com.authsystem.storageservice.repository.StoredObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageEngineService {

    private static final String KEY_ALGORITHM = "AES";
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final String BLOB_EXTENSION = ".blob";
    private static final String FOLDER_MARKER = ".folder";
    private static final String FOLDER_CONTENT_TYPE = "application/x-directory";

    private final ObjectBlobRepository objectBlobRepository;
    private final StoredObjectRepository storedObjectRepository;
    private final ObjectReferenceRepository objectReferenceRepository;
    private final ReplicationTaskRepository replicationTaskRepository;
    private final StorageWalRepository storageWalRepository;
    private final StorageSagaRepository storageSagaRepository;
    private final WalService walService;
    private final ReplicationTaskService replicationTaskService;
    private final ReplicationProducer replicationProducer;
    private final SagaService sagaService;
    private final StorageMetricsService storageMetricsService;

    @Value("${storage.local.root}")
    private String storageRoot;

    @Value("${storage.encryption.key}")
    private String encryptionKey;

    @Value("${storage.replication.enabled:false}")
    private boolean replicationEnabled;

    @Value("${storage.replication.target:minio}")
    private String replicationTarget;

    @Transactional
    public StoredObjectResponse upload(
            String ownerId,
            String objectKey,
            MultipartFile file
    ) {
        Instant startedAt = Instant.now();
        String operationId = UUID.randomUUID().toString();

        StorageSaga saga = sagaService.startSaga(
                SagaType.OBJECT_UPLOAD,
                ownerId + ":" + objectKey,
                "UPLOAD_STARTED"
        );

        StorageWal wal = walService.startUpload(operationId, ownerId, objectKey);

        try {
            validateUpload(ownerId, objectKey, file);

            byte[] originalBytes = file.getBytes();
            String contentHash = sha256(originalBytes);

            ObjectBlob blob = objectBlobRepository.findByContentHash(contentHash)
                    .orElseGet(() -> createEncryptedBlob(originalBytes, contentHash));

            blob.setReferenceCount(blob.getReferenceCount() + 1);
            objectBlobRepository.save(blob);

            StoredObject storedObject = StoredObject.builder()
                    .ownerId(ownerId)
                    .objectKey(objectKey)
                    .fileName(resolveFileName(file))
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .contentHash(contentHash)
                    .blob(blob)
                    .status(StoredObjectStatus.ACTIVE)
                    .version(1L)
                    .build();

            StoredObject savedObject = storedObjectRepository.save(storedObject);

            ObjectReference reference = ObjectReference.builder()
                    .object(savedObject)
                    .blob(blob)
                    .ownerId(ownerId)
                    .status(ObjectReferenceStatus.ACTIVE)
                    .build();

            objectReferenceRepository.save(reference);

            publishReplicationIfEnabled(savedObject, blob, ownerId, objectKey);

            walService.markCompleted(wal.getId(), savedObject.getId(), blob.getId());
            sagaService.markCompleted(saga.getId(), "UPLOAD_COMPLETED");

            log.info(
                    "ST_LOG | ACTION:OBJECT_UPLOAD_COMPLETED | OWNER_ID:{} | OBJECT_KEY:{} | OBJECT_ID:{} | BLOB_ID:{} | HASH:{}",
                    ownerId,
                    objectKey,
                    savedObject.getId(),
                    blob.getId(),
                    contentHash
            );

            storageMetricsService.recordUploadSuccess(Duration.between(startedAt, Instant.now()));
            return toResponse(savedObject);
        } catch (Exception ex) {
            walService.markFailed(wal.getId(), ex.getMessage());
            sagaService.markFailed(
                    saga.getId(),
                    "UPLOAD_FAILED",
                    ex.getMessage(),
                    true
            );

            log.error(
                    "ST_LOG | ACTION:OBJECT_UPLOAD_FAILED | OWNER_ID:{} | OBJECT_KEY:{} | MSG:{}",
                    ownerId,
                    objectKey,
                    ex.getMessage()
            );

            storageMetricsService.recordUploadFailure(Duration.between(startedAt, Instant.now()));
            throw new IllegalStateException("Object upload failed", ex);
        }
    }

    @Transactional(readOnly = true)
    public StoredObjectResponse getMetadata(String ownerId, String objectKey) {
        StoredObject storedObject = findActiveObject(ownerId, objectKey);

        log.info(
                "ST_LOG | ACTION:OBJECT_METADATA_READ | OWNER_ID:{} | OBJECT_KEY:{} | OBJECT_ID:{}",
                ownerId,
                objectKey,
                storedObject.getId()
        );

        return toResponse(storedObject);
    }

    @Transactional(readOnly = true)
    public List<StoredObjectDashboardResponse> listObjectsForOwner(String ownerId) {
        return storedObjectRepository.findByOwnerId(ownerId).stream()
                .sorted(Comparator.comparing(StoredObject::getUpdatedAt).reversed())
                .map(this::toDashboardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoredObjectDashboardResponse> listAllObjects() {
        return storedObjectRepository.findAll().stream()
                .sorted(Comparator.comparing(StoredObject::getUpdatedAt).reversed())
                .map(this::toDashboardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReplicationTaskDashboardResponse> listReplicationTasks() {
        return replicationTaskRepository.findAll().stream()
                .sorted(Comparator.comparing(ReplicationTask::getUpdatedAt).reversed())
                .map(task -> new ReplicationTaskDashboardResponse(
                        task.getId(),
                        task.getObject().getId(),
                        task.getObject().getOwnerId(),
                        task.getObject().getObjectKey(),
                        task.getBlob().getId(),
                        task.getTarget(),
                        task.getStatus().name(),
                        task.getRetryCount(),
                        task.getLastError(),
                        task.getNextRetryAt(),
                        task.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StorageWalDashboardResponse> listWalEntries() {
        return storageWalRepository.findAll().stream()
                .sorted(Comparator.comparing(StorageWal::getUpdatedAt).reversed())
                .map(wal -> new StorageWalDashboardResponse(
                        wal.getId(),
                        wal.getOperationId(),
                        wal.getOperationType().name(),
                        wal.getObjectId(),
                        wal.getBlobId(),
                        wal.getOwnerId(),
                        wal.getObjectKey(),
                        wal.getStatus().name(),
                        wal.getErrorMessage(),
                        wal.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StorageSagaDashboardResponse> listSagas() {
        return storageSagaRepository.findAll().stream()
                .sorted(Comparator.comparing(StorageSaga::getUpdatedAt).reversed())
                .map(saga -> new StorageSagaDashboardResponse(
                        saga.getId(),
                        saga.getSagaType().name(),
                        saga.getAggregateId(),
                        saga.getStatus().name(),
                        saga.getCurrentStep(),
                        saga.getCompensationRequired(),
                        saga.getErrorMessage(),
                        saga.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public StoredObjectResponse createFolder(String ownerId, String folderKey) {
        String normalizedFolderKey = normalizeFolderMarkerKey(folderKey);
        String folderPath = folderPathFromMarker(normalizedFolderKey);

        if (activeFolderPathExists(ownerId, folderPath)) {
            throw new IllegalArgumentException("Folder already exists");
        }

        byte[] markerBytes = new byte[0];
        String contentHash = sha256((ownerId + ":" + normalizedFolderKey).getBytes(StandardCharsets.UTF_8));
        ObjectBlob blob = createEncryptedBlob(markerBytes, contentHash);
        blob.setReferenceCount(blob.getReferenceCount() + 1);
        objectBlobRepository.save(blob);

        StoredObject folder = StoredObject.builder()
                .ownerId(ownerId)
                .objectKey(normalizedFolderKey)
                .fileName(FOLDER_MARKER)
                .contentType(FOLDER_CONTENT_TYPE)
                .sizeBytes(0L)
                .contentHash(contentHash)
                .blob(blob)
                .status(StoredObjectStatus.ACTIVE)
                .version(1L)
                .build();

        StoredObject savedFolder = storedObjectRepository.save(folder);

        objectReferenceRepository.save(ObjectReference.builder()
                .object(savedFolder)
                .blob(blob)
                .ownerId(ownerId)
                .status(ObjectReferenceStatus.ACTIVE)
                .build());

        log.info(
                "ST_LOG | ACTION:FOLDER_CREATED | OWNER_ID:{} | FOLDER_KEY:{} | OBJECT_ID:{}",
                ownerId,
                normalizedFolderKey,
                savedFolder.getId()
        );

        return toResponse(savedFolder);
    }

    @Transactional
    public StoredObjectResponse moveObject(String ownerId, String oldObjectKey, String newObjectKey) {
        return moveObject(ownerId, oldObjectKey, newObjectKey, FileConflictPolicy.FAIL);
    }

    @Transactional
    public StoredObjectResponse moveObject(String ownerId, String oldObjectKey, String newObjectKey, boolean overwrite) {
        return moveObject(ownerId, oldObjectKey, newObjectKey, resolveFileConflictPolicy(overwrite, null));
    }

    @Transactional
    public StoredObjectResponse moveObject(
            String ownerId,
            String oldObjectKey,
            String newObjectKey,
            FileConflictPolicy fileConflictPolicy
    ) {
        String normalizedOldKey = normalizeObjectKey(oldObjectKey);
        FileConflictPolicy resolvedPolicy = resolveFileConflictPolicy(false, fileConflictPolicy);

        if (isFolderMarker(normalizedOldKey)) {
            return moveFolder(ownerId, normalizedOldKey, newObjectKey, resolvedPolicy);
        }

        String normalizedNewKey = normalizeObjectKey(newObjectKey);
        StoredObject object = findActiveObject(ownerId, normalizedOldKey);

        if (normalizedOldKey.equals(normalizedNewKey)) {
            return toResponse(object);
        }

        String resolvedTargetKey = resolveFileTargetKey(
                ownerId,
                normalizedOldKey,
                normalizedNewKey,
                resolvedPolicy,
                new HashSet<>()
        );

        object.setObjectKey(resolvedTargetKey);
        object.setFileName(fileNameFromObjectKey(resolvedTargetKey));

        StoredObject savedObject = storedObjectRepository.save(object);

        log.info(
                "ST_LOG | ACTION:OBJECT_MOVED | OWNER_ID:{} | OLD_KEY:{} | NEW_KEY:{} | OBJECT_ID:{}",
                ownerId,
                normalizedOldKey,
                resolvedTargetKey,
                savedObject.getId()
        );

        return toResponse(savedObject);
    }

    private StoredObjectResponse moveFolder(
            String ownerId,
            String oldMarkerKey,
            String newObjectKey,
            FileConflictPolicy fileConflictPolicy
    ) {
        String normalizedNewMarkerKey = normalizeFolderMarkerKey(newObjectKey);
        String oldFolderPath = folderPathFromMarker(oldMarkerKey);
        String newFolderPath = folderPathFromMarker(normalizedNewMarkerKey);
        Optional<StoredObject> folderMarker = findActiveObjectOptional(ownerId, oldMarkerKey);

        List<StoredObject> objectsToMove = findActiveObjectsUnderFolder(ownerId, oldFolderPath);

        if (objectsToMove.isEmpty()) {
            throw new IllegalArgumentException("Folder not found");
        }

        if (oldFolderPath.equals(newFolderPath)) {
            return toResponse(folderMarker.orElse(objectsToMove.get(0)));
        }

        if (newFolderPath.startsWith(oldFolderPath + "/")) {
            throw new IllegalArgumentException("Folder cannot be moved into itself");
        }

        FolderOperationPlan plan = buildFolderOperationPlan(
                ownerId,
                objectsToMove,
                oldFolderPath,
                newFolderPath,
                fileConflictPolicy,
                true
        );

        plan.objectsToDeleteBeforeWrite().forEach(this::markStoredObjectDeleted);
        plan.sourceMarkersToDelete().forEach(this::markStoredObjectDeleted);
        storedObjectRepository.flush();

        List<StoredObject> movedObjects = new ArrayList<>();
        StoredObject movedFolderMarker = plan.responseObject()
                .orElse(folderMarker.orElse(objectsToMove.get(0)));

        for (ObjectTransfer transfer : plan.transfers()) {
            StoredObject object = transfer.sourceObject();
            object.setObjectKey(transfer.targetKey());
            object.setFileName(isFolderMarker(transfer.targetKey())
                    ? FOLDER_MARKER
                    : fileNameFromObjectKey(transfer.targetKey()));
            movedObjects.add(object);

            if (object.getObjectKey().equals(normalizedNewMarkerKey)) {
                movedFolderMarker = object;
            }
        }
        storedObjectRepository.saveAll(movedObjects);

        log.info(
                "ST_LOG | ACTION:FOLDER_MOVED | OWNER_ID:{} | OLD_FOLDER:{} | NEW_FOLDER:{} | MOVED_OBJECTS:{}",
                ownerId,
                oldFolderPath,
                newFolderPath,
                objectsToMove.size()
        );

        return toResponse(movedFolderMarker);
    }

    private String targetKeyForFolderMove(String sourceKey, String oldFolderPath, String newFolderPath) {
        if (sourceKey.equals(oldFolderPath + "/" + FOLDER_MARKER)) {
            return newFolderPath + "/" + FOLDER_MARKER;
        }

        String suffix = sourceKey.substring(oldFolderPath.length());
        return newFolderPath + suffix;
    }

    private String folderPathFromMarker(String markerKey) {
        String normalizedMarkerKey = normalizeFolderMarkerKey(markerKey);

        if (normalizedMarkerKey.equals(FOLDER_MARKER)) {
            return "";
        }

        return normalizedMarkerKey.substring(0, normalizedMarkerKey.length() - ("/" + FOLDER_MARKER).length());
    }

    @Transactional
    public StoredObjectResponse copyObject(String ownerId, String sourceObjectKey, String targetObjectKey) {
        return copyObject(ownerId, sourceObjectKey, targetObjectKey, FileConflictPolicy.FAIL);
    }

    @Transactional
    public StoredObjectResponse copyObject(String ownerId, String sourceObjectKey, String targetObjectKey, boolean overwrite) {
        return copyObject(ownerId, sourceObjectKey, targetObjectKey, resolveFileConflictPolicy(overwrite, null));
    }

    @Transactional
    public StoredObjectResponse copyObject(
            String ownerId,
            String sourceObjectKey,
            String targetObjectKey,
            FileConflictPolicy fileConflictPolicy
    ) {
        String normalizedSourceKey = normalizeObjectKey(sourceObjectKey);
        FileConflictPolicy resolvedPolicy = resolveFileConflictPolicy(false, fileConflictPolicy);

        if (isFolderMarker(normalizedSourceKey)) {
            return copyFolder(ownerId, normalizedSourceKey, targetObjectKey, resolvedPolicy);
        }

        StoredObject sourceObject = findActiveObject(ownerId, normalizedSourceKey);
        String normalizedTargetKey = normalizeObjectKey(targetObjectKey);
        String resolvedTargetKey = resolveFileTargetKey(
                ownerId,
                normalizedSourceKey,
                normalizedTargetKey,
                resolvedPolicy,
                new HashSet<>()
        );

        StoredObject copiedObject = createObjectCopy(ownerId, sourceObject, resolvedTargetKey);

        log.info(
                "ST_LOG | ACTION:OBJECT_COPIED | OWNER_ID:{} | SOURCE_KEY:{} | TARGET_KEY:{} | OBJECT_ID:{}",
                ownerId,
                normalizedSourceKey,
                resolvedTargetKey,
                copiedObject.getId()
        );

        return toResponse(copiedObject);
    }

    private StoredObjectResponse copyFolder(
            String ownerId,
            String sourceMarkerKey,
            String targetObjectKey,
            FileConflictPolicy fileConflictPolicy
    ) {
        String sourceFolderPath = folderPathFromMarker(sourceMarkerKey);
        String targetMarkerKey = normalizeFolderMarkerKey(targetObjectKey);
        String targetFolderPath = folderPathFromMarker(targetMarkerKey);

        if (targetFolderPath.equals(sourceFolderPath) && fileConflictPolicy == FileConflictPolicy.KEEP_BOTH) {
            targetFolderPath = uniqueAvailableFolderPath(ownerId, sourceFolderPath);
            targetMarkerKey = folderMarkerKeyForPath(targetFolderPath);
        }

        if (targetFolderPath.equals(sourceFolderPath) || targetFolderPath.startsWith(sourceFolderPath + "/")) {
            throw new IllegalArgumentException("Folder cannot be copied into itself");
        }

        List<StoredObject> sourceObjects = findActiveObjectsUnderFolder(ownerId, sourceFolderPath);

        if (sourceObjects.isEmpty()) {
            throw new IllegalArgumentException("Folder not found");
        }

        FolderOperationPlan plan = buildFolderOperationPlan(
                ownerId,
                sourceObjects,
                sourceFolderPath,
                targetFolderPath,
                fileConflictPolicy,
                false
        );

        StoredObject firstCopiedObject = null;

        plan.objectsToDeleteBeforeWrite().forEach(this::markStoredObjectDeleted);
        storedObjectRepository.flush();

        for (ObjectTransfer transfer : plan.transfers()) {
            StoredObject copiedObject = createObjectCopy(ownerId, transfer.sourceObject(), transfer.targetKey());
            if (firstCopiedObject == null || transfer.targetKey().equals(targetMarkerKey)) {
                firstCopiedObject = copiedObject;
            }
        }

        if (firstCopiedObject == null) {
            String finalTargetMarkerKey = targetMarkerKey;
            firstCopiedObject = plan.responseObject()
                    .orElseGet(() -> findActiveObject(ownerId, finalTargetMarkerKey));
        }

        log.info(
                "ST_LOG | ACTION:FOLDER_COPIED | OWNER_ID:{} | SOURCE_FOLDER:{} | TARGET_FOLDER:{} | COPIED_OBJECTS:{}",
                ownerId,
                sourceFolderPath,
                targetFolderPath,
                sourceObjects.size()
        );

        return toResponse(firstCopiedObject);
    }

    @Transactional(readOnly = true)
    public ObjectOperationPreviewResponse previewObjectOperation(
            String ownerId,
            String operationType,
            String sourceObjectKey,
            String targetObjectKey
    ) {
        String normalizedSourceKey = normalizeObjectKey(sourceObjectKey);
        boolean moveOperation = operationType == null || operationType.equalsIgnoreCase("move");

        if (isFolderMarker(normalizedSourceKey)) {
            String sourceFolderPath = folderPathFromMarker(normalizedSourceKey);
            String targetFolderPath = folderPathFromMarker(normalizeFolderMarkerKey(targetObjectKey));

            if (targetFolderPath.equals(sourceFolderPath)) {
                return new ObjectOperationPreviewResponse(List.of(), List.of(), moveOperation ? null : "Folder cannot be copied into itself");
            }
            if (targetFolderPath.startsWith(sourceFolderPath + "/")) {
                return new ObjectOperationPreviewResponse(List.of(), List.of(), "Folder cannot be moved or copied into itself");
            }

            List<StoredObject> sourceObjects = findActiveObjectsUnderFolder(ownerId, sourceFolderPath);
            List<ObjectOperationConflict> fileConflicts = collectFolderFileConflicts(
                    ownerId,
                    sourceObjects,
                    sourceFolderPath,
                    targetFolderPath
            );
            List<String> folderMerges = activeFolderPathExists(ownerId, targetFolderPath)
                    ? List.of(targetFolderPath)
                    : List.of();

            return new ObjectOperationPreviewResponse(fileConflicts, folderMerges, null);
        }

        String normalizedTargetKey = normalizeObjectKey(targetObjectKey);
        if (moveOperation && normalizedSourceKey.equals(normalizedTargetKey)) {
            return new ObjectOperationPreviewResponse(List.of(), List.of(), null);
        }

        List<ObjectOperationConflict> conflicts = findActiveObjectOptional(ownerId, normalizedTargetKey)
                .map(target -> List.of(new ObjectOperationConflict(normalizedSourceKey, normalizedTargetKey, "file")))
                .orElse(List.of());

        return new ObjectOperationPreviewResponse(conflicts, List.of(), null);
    }

    private boolean activeFolderPathExists(String ownerId, String folderPath) {
        return !findActiveObjectsUnderFolder(ownerId, folderPath).isEmpty();
    }

    @Transactional
    public void deleteFolder(String ownerId, String folderKey, boolean recursive) {
        String normalizedMarkerKey = normalizeFolderMarkerKey(folderKey);
        String folderPath = folderPathFromMarker(normalizedMarkerKey);
        List<StoredObject> objectsToDelete = findActiveObjectsUnderFolder(ownerId, folderPath);

        if (objectsToDelete.isEmpty()) {
            throw new IllegalArgumentException("Folder not found");
        }

        boolean hasChildren = objectsToDelete.stream()
                .anyMatch(object -> !object.getObjectKey().equals(normalizedMarkerKey));

        if (hasChildren && !recursive) {
            throw new IllegalArgumentException("Folder is not empty");
        }

        objectsToDelete.forEach(this::markStoredObjectDeleted);

        log.info(
                "ST_LOG | ACTION:FOLDER_DELETE_COMPLETED | OWNER_ID:{} | FOLDER_KEY:{} | DELETED_OBJECTS:{} | RECURSIVE:{}",
                ownerId,
                folderPath,
                objectsToDelete.size(),
                recursive
        );
        storageMetricsService.recordDeleteSuccess();
    }

    private FileConflictPolicy resolveFileConflictPolicy(Boolean overwrite, FileConflictPolicy fileConflictPolicy) {
        if (fileConflictPolicy != null) {
            return fileConflictPolicy;
        }

        return Boolean.TRUE.equals(overwrite) ? FileConflictPolicy.REPLACE : FileConflictPolicy.FAIL;
    }

    private String resolveFileTargetKey(
            String ownerId,
            String sourceKey,
            String targetKey,
            FileConflictPolicy fileConflictPolicy,
            Set<String> reservedTargetKeys
    ) {
        Optional<StoredObject> targetObject = findActiveObjectOptional(ownerId, targetKey);
        boolean reservedByCurrentPlan = reservedTargetKeys.contains(targetKey);

        if (targetObject.isEmpty() && !reservedByCurrentPlan) {
            reservedTargetKeys.add(targetKey);
            return targetKey;
        }

        if (targetObject.map(StoredObject::getObjectKey).filter(sourceKey::equals).isPresent()) {
            reservedTargetKeys.add(targetKey);
            return targetKey;
        }

        if (fileConflictPolicy == FileConflictPolicy.REPLACE) {
            targetObject.ifPresent(this::markStoredObjectDeleted);
            storedObjectRepository.flush();
            reservedTargetKeys.add(targetKey);
            return targetKey;
        }

        if (fileConflictPolicy == FileConflictPolicy.KEEP_BOTH) {
            String uniqueTargetKey = uniqueAvailableObjectKey(ownerId, targetKey, reservedTargetKeys);
            reservedTargetKeys.add(uniqueTargetKey);
            return uniqueTargetKey;
        }

        throw new ObjectOperationConflictException(List.of(
                new ObjectOperationConflict(sourceKey, targetKey, "file")
        ));
    }

    private FolderOperationPlan buildFolderOperationPlan(
            String ownerId,
            List<StoredObject> sourceObjects,
            String sourceFolderPath,
            String targetFolderPath,
            FileConflictPolicy fileConflictPolicy,
            boolean moveOperation
    ) {
        Set<String> sourceKeys = sourceObjects.stream()
                .map(StoredObject::getObjectKey)
                .collect(Collectors.toSet());
        Set<String> reservedTargetKeys = new HashSet<>();
        List<ObjectTransfer> transfers = new ArrayList<>();
        List<StoredObject> objectsToDeleteBeforeWrite = new ArrayList<>();
        List<StoredObject> sourceMarkersToDelete = new ArrayList<>();
        List<ObjectOperationConflict> conflicts = new ArrayList<>();
        Optional<StoredObject> responseObject = Optional.empty();

        for (StoredObject sourceObject : sourceObjects) {
            String sourceKey = sourceObject.getObjectKey();
            String targetKey = targetKeyForFolderMove(sourceKey, sourceFolderPath, targetFolderPath);

            if (isFolderMarker(targetKey)) {
                Optional<StoredObject> existingFolderMarker = findActiveObjectOptional(ownerId, targetKey);
                if (existingFolderMarker.isPresent() && !sourceKeys.contains(targetKey)) {
                    if (moveOperation) {
                        sourceMarkersToDelete.add(sourceObject);
                    }
                    if (sourceKey.equals(folderMarkerKeyForPath(sourceFolderPath))) {
                        responseObject = existingFolderMarker;
                    }
                    continue;
                }

                if (reservedTargetKeys.add(targetKey)) {
                    transfers.add(new ObjectTransfer(sourceObject, targetKey));
                } else if (moveOperation) {
                    sourceMarkersToDelete.add(sourceObject);
                }
                continue;
            }

            Optional<StoredObject> existingTarget = findActiveObjectOptional(ownerId, targetKey);
            boolean targetOwnedBySourceTree = existingTarget
                    .map(StoredObject::getObjectKey)
                    .filter(sourceKeys::contains)
                    .isPresent();

            if ((existingTarget.isPresent() && !targetOwnedBySourceTree) || reservedTargetKeys.contains(targetKey)) {
                if (fileConflictPolicy == FileConflictPolicy.FAIL) {
                    conflicts.add(new ObjectOperationConflict(sourceKey, targetKey, "file"));
                    continue;
                }

                if (fileConflictPolicy == FileConflictPolicy.REPLACE) {
                    existingTarget.ifPresent(objectsToDeleteBeforeWrite::add);
                }

                if (fileConflictPolicy == FileConflictPolicy.KEEP_BOTH) {
                    targetKey = uniqueAvailableObjectKey(ownerId, targetKey, reservedTargetKeys);
                }
            }

            reservedTargetKeys.add(targetKey);
            transfers.add(new ObjectTransfer(sourceObject, targetKey));
        }

        if (!conflicts.isEmpty()) {
            throw new ObjectOperationConflictException(conflicts);
        }

        return new FolderOperationPlan(
                transfers,
                objectsToDeleteBeforeWrite,
                sourceMarkersToDelete,
                responseObject
        );
    }

    private List<ObjectOperationConflict> collectFolderFileConflicts(
            String ownerId,
            List<StoredObject> sourceObjects,
            String sourceFolderPath,
            String targetFolderPath
    ) {
        Set<String> sourceKeys = sourceObjects.stream()
                .map(StoredObject::getObjectKey)
                .collect(Collectors.toSet());

        return sourceObjects.stream()
                .filter(sourceObject -> !isFolderMarker(sourceObject.getObjectKey()))
                .map(sourceObject -> {
                    String targetKey = targetKeyForFolderMove(
                            sourceObject.getObjectKey(),
                            sourceFolderPath,
                            targetFolderPath
                    );
                    return findActiveObjectOptional(ownerId, targetKey)
                            .filter(target -> !sourceKeys.contains(target.getObjectKey()))
                            .map(target -> new ObjectOperationConflict(sourceObject.getObjectKey(), targetKey, "file"))
                            .orElse(null);
                })
                .filter(conflict -> conflict != null)
                .toList();
    }

    private String uniqueAvailableObjectKey(String ownerId, String targetKey, Set<String> reservedTargetKeys) {
        String targetParent = parentPath(targetKey);
        String targetName = fileNameFromObjectKey(targetKey);

        for (int attempt = 1; attempt < 1000; attempt += 1) {
            String candidate = joinPath(targetParent, copyNameForAttempt(targetName, attempt, false));
            if (!reservedTargetKeys.contains(candidate) && findActiveObjectOptional(ownerId, candidate).isEmpty()) {
                return candidate;
            }
        }

        return joinPath(targetParent, copyNameForAttempt(targetName, (int) (System.currentTimeMillis() % 100000), false));
    }

    private String uniqueAvailableFolderPath(String ownerId, String folderPath) {
        String targetParent = parentPath(folderPath);
        String targetName = fileNameFromObjectKey(folderPath);

        for (int attempt = 1; attempt < 1000; attempt += 1) {
            String candidate = joinPath(targetParent, copyNameForAttempt(targetName, attempt, true));
            if (!activeFolderPathExists(ownerId, candidate)) {
                return candidate;
            }
        }

        return joinPath(targetParent, copyNameForAttempt(targetName, (int) (System.currentTimeMillis() % 100000), true));
    }

    private String copyNameForAttempt(String name, int attempt, boolean folder) {
        String suffix = attempt == 1 ? " (copy)" : " (copy " + attempt + ")";
        if (folder) {
            return name + suffix;
        }

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0) {
            return name + suffix;
        }

        return name.substring(0, dotIndex) + suffix + name.substring(dotIndex);
    }

    private String parentPath(String objectKey) {
        String normalizedKey = objectKey == null ? "" : objectKey.replace("\\", "/").replaceAll("^/+|/+$", "");
        int slashIndex = normalizedKey.lastIndexOf('/');
        return slashIndex < 0 ? "" : normalizedKey.substring(0, slashIndex);
    }

    private String joinPath(String parent, String child) {
        if (parent == null || parent.isBlank()) {
            return child == null ? "" : child.replaceAll("^/+|/+$", "");
        }
        if (child == null || child.isBlank()) {
            return parent.replaceAll("^/+|/+$", "");
        }

        return (parent + "/" + child).replace("\\", "/").replaceAll("/+", "/").replaceAll("^/+|/+$", "");
    }

    private String folderMarkerKeyForPath(String folderPath) {
        return joinPath(folderPath, FOLDER_MARKER);
    }

    private List<StoredObject> findActiveObjectsUnderFolder(String ownerId, String folderPath) {
        return storedObjectRepository.findByOwnerIdAndObjectKeyStartingWithAndStatus(
                ownerId,
                folderPath.isBlank() ? "" : folderPath + "/",
                StoredObjectStatus.ACTIVE
        );
    }

    private Optional<StoredObject> findActiveObjectOptional(String ownerId, String objectKey) {
        return storedObjectRepository.findByOwnerIdAndObjectKeyAndStatus(
                ownerId,
                objectKey,
                StoredObjectStatus.ACTIVE
        );
    }

    private void markStoredObjectDeleted(StoredObject storedObject) {
        ObjectBlob blob = storedObject.getBlob();
        storedObject.setStatus(StoredObjectStatus.DELETED);
        storedObjectRepository.save(storedObject);

        objectReferenceRepository.findByObject_Id(storedObject.getId())
                .ifPresent(reference -> {
                    reference.setStatus(ObjectReferenceStatus.DELETED);
                    objectReferenceRepository.save(reference);
                });

        blob.setReferenceCount(Math.max(0, blob.getReferenceCount() - 1));
        if (blob.getReferenceCount() == 0) {
            blob.setStatus(BlobStatus.GC_PENDING);
        }
        objectBlobRepository.save(blob);
    }

    private StoredObject createObjectCopy(String ownerId, StoredObject sourceObject, String targetKey) {
        ObjectBlob blob = sourceObject.getBlob();
        blob.setReferenceCount(blob.getReferenceCount() + 1);
        objectBlobRepository.save(blob);

        StoredObject copiedObject = StoredObject.builder()
                .ownerId(ownerId)
                .objectKey(targetKey)
                .fileName(isFolderMarker(targetKey) ? FOLDER_MARKER : fileNameFromObjectKey(targetKey))
                .contentType(sourceObject.getContentType())
                .sizeBytes(sourceObject.getSizeBytes())
                .contentHash(sourceObject.getContentHash())
                .blob(blob)
                .status(StoredObjectStatus.ACTIVE)
                .version(1L)
                .build();

        StoredObject savedObject = storedObjectRepository.save(copiedObject);

        objectReferenceRepository.save(ObjectReference.builder()
                .object(savedObject)
                .blob(blob)
                .ownerId(ownerId)
                .status(ObjectReferenceStatus.ACTIVE)
                .build());

        return savedObject;
    }

    @Transactional(readOnly = true)
    public ObjectDownloadResponse download(String ownerId, String objectKey) {
        Instant startedAt = Instant.now();

        try {
            StoredObject storedObject = findActiveObject(ownerId, objectKey);
            ObjectBlob blob = storedObject.getBlob();

            if (blob.getStatus() != BlobStatus.ACTIVE) {
                throw new IllegalStateException("Blob is not active");
            }

            byte[] encryptedBytes = Files.readAllBytes(Path.of(blob.getStoragePath()));
            byte[] plainBytes = decrypt(encryptedBytes, blob.getEncryptionIv());

            log.info(
                    "ST_LOG | ACTION:OBJECT_DOWNLOAD_COMPLETED | OWNER_ID:{} | OBJECT_KEY:{} | OBJECT_ID:{} | BLOB_ID:{}",
                    ownerId,
                    objectKey,
                    storedObject.getId(),
                    blob.getId()
            );

            storageMetricsService.recordDownloadSuccess(Duration.between(startedAt, Instant.now()));
            return new ObjectDownloadResponse(
                    plainBytes,
                    storedObject.getFileName(),
                    storedObject.getContentType(),
                    storedObject.getSizeBytes()
            );
        } catch (IOException ex) {
            log.error(
                    "ST_LOG | ACTION:OBJECT_DOWNLOAD_FAILED | OWNER_ID:{} | OBJECT_KEY:{} | MSG:{}",
                    ownerId,
                    objectKey,
                    ex.getMessage()
            );

            storageMetricsService.recordDownloadFailure(Duration.between(startedAt, Instant.now()));
            throw new IllegalStateException("Object download failed", ex);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error(
                    "ST_LOG | ACTION:OBJECT_DOWNLOAD_FAILED | OWNER_ID:{} | OBJECT_KEY:{} | MSG:{}",
                    ownerId,
                    objectKey,
                    ex.getMessage()
            );

            storageMetricsService.recordDownloadFailure(Duration.between(startedAt, Instant.now()));
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "ST_LOG | ACTION:OBJECT_DECRYPT_FAILED | OWNER_ID:{} | OBJECT_KEY:{} | MSG:{}",
                    ownerId,
                    objectKey,
                    ex.getMessage()
            );

            storageMetricsService.recordDownloadFailure(Duration.between(startedAt, Instant.now()));
            throw new IllegalStateException("Object decrypt failed", ex);
        }
    }

    @Transactional
    public void delete(String ownerId, String objectKey) {
        StoredObject storedObject = findActiveObject(ownerId, objectKey);
        ObjectBlob blob = storedObject.getBlob();

        StorageSaga saga = sagaService.startSaga(
                SagaType.OBJECT_DELETE,
                storedObject.getId().toString(),
                "DELETE_STARTED"
        );

        StorageWal wal = walService.startDelete(ownerId, objectKey, storedObject.getId(), blob.getId());

        try {
            storedObject.setStatus(StoredObjectStatus.DELETED);
            storedObjectRepository.save(storedObject);

            objectReferenceRepository.findByObject_Id(storedObject.getId())
                    .ifPresent(reference -> {
                        reference.setStatus(ObjectReferenceStatus.DELETED);
                        objectReferenceRepository.save(reference);
                    });

            blob.setReferenceCount(Math.max(0, blob.getReferenceCount() - 1));

            if (blob.getReferenceCount() == 0) {
                blob.setStatus(BlobStatus.GC_PENDING);
            }

            objectBlobRepository.save(blob);

            walService.markCompleted(wal.getId(), storedObject.getId(), blob.getId());
            sagaService.markCompleted(saga.getId(), "DELETE_COMPLETED");

            log.info(
                    "ST_LOG | ACTION:OBJECT_DELETE_COMPLETED | OWNER_ID:{} | OBJECT_KEY:{} | OBJECT_ID:{} | BLOB_ID:{} | BLOB_STATUS:{}",
                    ownerId,
                    objectKey,
                    storedObject.getId(),
                    blob.getId(),
                    blob.getStatus()
            );
            storageMetricsService.recordDeleteSuccess();
        } catch (Exception ex) {
            walService.markFailed(wal.getId(), ex.getMessage());
            sagaService.markFailed(
                    saga.getId(),
                    "DELETE_FAILED",
                    ex.getMessage(),
                    true
            );

            log.error(
                    "ST_LOG | ACTION:OBJECT_DELETE_FAILED | OWNER_ID:{} | OBJECT_KEY:{} | MSG:{}",
                    ownerId,
                    objectKey,
                    ex.getMessage()
            );

            storageMetricsService.recordDeleteFailure();
            throw new IllegalStateException("Object delete failed", ex);
        }
    }


    private ObjectBlob createEncryptedBlob(byte[] originalBytes, String contentHash) {
        try {
            EncryptedBlobContent encryptedContent = encrypt(originalBytes);
            Path blobPath = resolveBlobPath(contentHash);

            Files.createDirectories(blobPath.getParent());
            Files.write(blobPath, encryptedContent.encryptedBytes());

            ObjectBlob blob = ObjectBlob.builder()
                    .contentHash(contentHash)
                    .storagePath(blobPath.toString())
                    .encryptedSizeBytes((long) encryptedContent.encryptedBytes().length)
                    .originalSizeBytes((long) originalBytes.length)
                    .encryptionAlgorithm(ENCRYPTION_ALGORITHM)
                    .encryptionIv(encryptedContent.iv())
                    .status(BlobStatus.ACTIVE)
                    .referenceCount(0L)
                    .build();

            ObjectBlob savedBlob = objectBlobRepository.save(blob);

            log.info(
                    "ST_LOG | ACTION:BLOB_CREATED | BLOB_ID:{} | HASH:{} | PATH:{}",
                    savedBlob.getId(),
                    contentHash,
                    blobPath
            );

            return savedBlob;
        } catch (Exception ex) {
            throw new IllegalStateException("Encrypted blob creation failed", ex);
        }
    }

    private void publishReplicationIfEnabled(
            StoredObject savedObject,
            ObjectBlob blob,
            String ownerId,
            String objectKey
    ) {
        if (!replicationEnabled) {
            log.info(
                    "ST_LOG | ACTION:OBJECT_REPLICATION_SKIPPED | REASON:DISABLED | OBJECT_ID:{} | BLOB_ID:{}",
                    savedObject.getId(),
                    blob.getId()
            );
            return;
        }

        ReplicationTask replicationTask = replicationTaskService.createPendingTask(
                savedObject,
                blob,
                replicationTarget
        );

        publishAfterCommit(replicationTask, savedObject, blob, ownerId, objectKey);
    }

    private void publishAfterCommit(
            ReplicationTask replicationTask,
            StoredObject savedObject,
            ObjectBlob blob,
            String ownerId,
            String objectKey
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishReplication(replicationTask, savedObject, blob, ownerId, objectKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishReplication(replicationTask, savedObject, blob, ownerId, objectKey);
            }
        });
    }

    private void publishReplication(
            ReplicationTask replicationTask,
            StoredObject savedObject,
            ObjectBlob blob,
            String ownerId,
            String objectKey
    ) {
        replicationProducer.publishObjectReplication(
                replicationTask.getId(),
                savedObject.getId(),
                blob.getId(),
                ownerId,
                objectKey,
                blob.getStoragePath(),
                replicationTask.getTarget()
        );
    }

    private void validateUpload(String ownerId, String objectKey, MultipartFile file) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId is required");
        }

        validateObjectKey(objectKey, "objectKey");

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        boolean objectExists = storedObjectRepository.existsByOwnerIdAndObjectKeyAndStatus(
                ownerId,
                objectKey,
                StoredObjectStatus.ACTIVE
        );
        if (objectExists) {
            throw new IllegalArgumentException("objectKey already exists for owner");
        }
    }

    private Path resolveBlobPath(String contentHash) {
        String firstLevel = contentHash.substring(0, 2);
        String secondLevel = contentHash.substring(2, 4);

        return Path.of(
                storageRoot,
                firstLevel,
                secondLevel,
                contentHash + BLOB_EXTENSION
        );
    }

    private String resolveFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            return "unnamed";
        }

        return originalFilename;
    }

    private String normalizeFolderMarkerKey(String folderKey) {
        String normalizedKey = normalizeObjectKey(folderKey);

        if (normalizedKey.equals(FOLDER_MARKER) || normalizedKey.endsWith("/" + FOLDER_MARKER)) {
            return normalizedKey;
        }

        return normalizedKey + "/" + FOLDER_MARKER;
    }

    private String normalizeObjectKey(String objectKey) {
        validateObjectKey(objectKey, "objectKey");

        String normalizedKey = objectKey.trim().replace("\\", "/");
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }
        while (normalizedKey.contains("//")) {
            normalizedKey = normalizedKey.replace("//", "/");
        }

        if (normalizedKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }

        return normalizedKey;
    }

    private void validateObjectKey(String objectKey, String fieldName) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (objectKey.contains("..")) {
            throw new IllegalArgumentException(fieldName + " cannot contain '..'");
        }
    }

    private boolean isFolderMarker(String objectKey) {
        return objectKey != null && (objectKey.equals(FOLDER_MARKER) || objectKey.endsWith("/" + FOLDER_MARKER));
    }

    private String fileNameFromObjectKey(String objectKey) {
        int slashIndex = objectKey.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == objectKey.length() - 1) {
            return objectKey;
        }

        return objectKey.substring(slashIndex + 1);
    }

    private EncryptedBlobContent encrypt(byte[] plainBytes) throws Exception {
        byte[] iv = generateIv();
        SecretKeySpec keySpec = new SecretKeySpec(normalizedEncryptionKey(), KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return new EncryptedBlobContent(
                cipher.doFinal(plainBytes),
                Base64.getEncoder().encodeToString(iv)
        );
    }

    private byte[] normalizedEncryptionKey() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length != 32) {
            throw new IllegalStateException("storage.encryption.key must be exactly 32 bytes for AES-256");
        }

        return keyBytes;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(bytes);
            return HexFormat.of().formatHex(hashedBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 hash calculation failed", ex);
        }
    }

    private StoredObjectResponse toResponse(StoredObject object) {
        return new StoredObjectResponse(
                object.getId(),
                object.getOwnerId(),
                object.getObjectKey(),
                object.getFileName(),
                object.getContentType(),
                object.getSizeBytes(),
                object.getContentHash(),
                object.getStatus().name(),
                object.getCreatedAt()
        );
    }

    private StoredObjectDashboardResponse toDashboardResponse(StoredObject object) {
        return new StoredObjectDashboardResponse(
                object.getId(),
                object.getOwnerId(),
                object.getObjectKey(),
                object.getFileName(),
                object.getContentType(),
                object.getSizeBytes(),
                object.getContentHash(),
                object.getStatus().name(),
                object.getCreatedAt(),
                object.getUpdatedAt()
        );
    }

    private StoredObject findActiveObject(String ownerId, String objectKey) {
        return findActiveObjectOptional(ownerId, objectKey)
                .orElseThrow(() -> new IllegalArgumentException("Object not found"));
    }

    private byte[] decrypt(byte[] encryptedBytes, String encodedIv) throws Exception {
        byte[] iv = Base64.getDecoder().decode(encodedIv);
        SecretKeySpec keySpec = new SecretKeySpec(normalizedEncryptionKey(), KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(encryptedBytes);
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private record ObjectTransfer(StoredObject sourceObject, String targetKey) {
    }

    private record FolderOperationPlan(
            List<ObjectTransfer> transfers,
            List<StoredObject> objectsToDeleteBeforeWrite,
            List<StoredObject> sourceMarkersToDelete,
            Optional<StoredObject> responseObject
    ) {
    }

    private record EncryptedBlobContent(byte[] encryptedBytes, String iv) {
    }
}
