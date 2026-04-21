package com.authsystem.storageservice.controller;

import com.authsystem.storageservice.dto.CreateFolderRequest;
import com.authsystem.storageservice.dto.CopyObjectRequest;
import com.authsystem.storageservice.dto.FileConflictPolicy;
import com.authsystem.storageservice.dto.MoveObjectRequest;
import com.authsystem.storageservice.dto.ObjectDownloadResponse;
import com.authsystem.storageservice.dto.ObjectOperationPreviewRequest;
import com.authsystem.storageservice.dto.ObjectOperationPreviewResponse;
import com.authsystem.storageservice.dto.ReplicationTaskDashboardResponse;
import com.authsystem.storageservice.dto.StorageSagaDashboardResponse;
import com.authsystem.storageservice.dto.StorageWalDashboardResponse;
import com.authsystem.storageservice.dto.StoredObjectDashboardResponse;
import com.authsystem.storageservice.dto.StoredObjectResponse;
import com.authsystem.storageservice.exception.ObjectOperationConflictException;
import com.authsystem.storageservice.service.StorageEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/objects")
public class ObjectStorageController {

    private final StorageEngineService storageEngineService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoredObjectResponse> upload(
            @RequestParam(required = false) String ownerId,
            @RequestParam String objectKey,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        StoredObjectResponse response = storageEngineService.upload(
                resolveOwnerId(ownerId, authenticatedUser, authenticatedRole),
                objectKey,
                file
        );
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<StoredObjectDashboardResponse>> myObjects(
            @RequestHeader("X-Authenticated-User") String authenticatedUser
    ) {
        return ResponseEntity.ok(storageEngineService.listObjectsForOwner(authenticatedUser));
    }

    @PostMapping("/folders")
    public ResponseEntity<StoredObjectResponse> createFolder(
            @RequestBody CreateFolderRequest request,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        StoredObjectResponse response = storageEngineService.createFolder(
                resolveOwnerId(request.ownerId(), authenticatedUser, authenticatedRole),
                request.folderKey()
        );
        return ResponseEntity.status(201).body(response);
    }

    @PatchMapping("/move")
    public ResponseEntity<?> moveObject(
            @RequestBody MoveObjectRequest request,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        try {
            StoredObjectResponse response = storageEngineService.moveObject(
                    resolveOwnerId(request.ownerId(), authenticatedUser, authenticatedRole),
                    request.oldObjectKey(),
                    request.newObjectKey(),
                    resolvePolicy(request.overwrite(), request.fileConflictPolicy())
            );
            return ResponseEntity.ok(response);
        } catch (ObjectOperationConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ObjectOperationPreviewResponse(ex.getConflicts(), List.of(), null));
        }
    }

    @PostMapping("/copy")
    public ResponseEntity<?> copyObject(
            @RequestBody CopyObjectRequest request,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        try {
            StoredObjectResponse response = storageEngineService.copyObject(
                    resolveOwnerId(request.ownerId(), authenticatedUser, authenticatedRole),
                    request.sourceObjectKey(),
                    request.targetObjectKey(),
                    resolvePolicy(request.overwrite(), request.fileConflictPolicy())
            );
            return ResponseEntity.status(201).body(response);
        } catch (ObjectOperationConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ObjectOperationPreviewResponse(ex.getConflicts(), List.of(), null));
        }
    }

    @PostMapping("/operations/preview")
    public ResponseEntity<ObjectOperationPreviewResponse> previewObjectOperation(
            @RequestBody ObjectOperationPreviewRequest request,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        return ResponseEntity.ok(storageEngineService.previewObjectOperation(
                resolveOwnerId(request.ownerId(), authenticatedUser, authenticatedRole),
                request.operationType(),
                request.sourceObjectKey(),
                request.targetObjectKey()
        ));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<StoredObjectDashboardResponse>> allObjects(
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        requireAdmin(authenticatedRole);
        return ResponseEntity.ok(storageEngineService.listAllObjects());
    }

    @GetMapping("/admin/replication-tasks")
    public ResponseEntity<List<ReplicationTaskDashboardResponse>> replicationTasks(
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        requireAdmin(authenticatedRole);
        return ResponseEntity.ok(storageEngineService.listReplicationTasks());
    }

    @GetMapping("/admin/wal")
    public ResponseEntity<List<StorageWalDashboardResponse>> walEntries(
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        requireAdmin(authenticatedRole);
        return ResponseEntity.ok(storageEngineService.listWalEntries());
    }

    @GetMapping("/admin/sagas")
    public ResponseEntity<List<StorageSagaDashboardResponse>> sagas(
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        requireAdmin(authenticatedRole);
        return ResponseEntity.ok(storageEngineService.listSagas());
    }

    @GetMapping("/metadata")
    public ResponseEntity<StoredObjectResponse> metadata(
            @RequestParam(required = false) String ownerId,
            @RequestParam String objectKey,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        StoredObjectResponse response = storageEngineService.getMetadata(
                resolveOwnerId(ownerId, authenticatedUser, authenticatedRole),
                objectKey
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(required = false) String ownerId,
            @RequestParam String objectKey,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        ObjectDownloadResponse response = storageEngineService.download(
                resolveOwnerId(ownerId, authenticatedUser, authenticatedRole),
                objectKey
        );

        MediaType mediaType = resolveMediaType(response.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(response.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(response.fileName())
                                .build()
                                .toString()
                )
                .body(response.content());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam(required = false) String ownerId,
            @RequestParam String objectKey,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        storageEngineService.delete(resolveOwnerId(ownerId, authenticatedUser, authenticatedRole), objectKey);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/folders")
    public ResponseEntity<Void> deleteFolder(
            @RequestParam(required = false) String ownerId,
            @RequestParam String folderKey,
            @RequestParam(defaultValue = "false") boolean recursive,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String authenticatedRole
    ) {
        storageEngineService.deleteFolder(
                resolveOwnerId(ownerId, authenticatedUser, authenticatedRole),
                folderKey,
                recursive
        );
        return ResponseEntity.noContent().build();
    }

    private String resolveOwnerId(String requestedOwnerId, String authenticatedUser, String authenticatedRole) {
        if ("ROLE_ADMIN".equals(authenticatedRole) && requestedOwnerId != null && !requestedOwnerId.isBlank()) {
            return requestedOwnerId;
        }

        if (authenticatedUser != null && !authenticatedUser.isBlank()) {
            return authenticatedUser;
        }

        if (requestedOwnerId != null && !requestedOwnerId.isBlank()) {
            return requestedOwnerId;
        }

        throw new IllegalArgumentException("ownerId is required");
    }

    private void requireAdmin(String authenticatedRole) {
        if (!"ROLE_ADMIN".equals(authenticatedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    private FileConflictPolicy resolvePolicy(Boolean overwrite, FileConflictPolicy fileConflictPolicy) {
        if (fileConflictPolicy != null) {
            return fileConflictPolicy;
        }

        return Boolean.TRUE.equals(overwrite) ? FileConflictPolicy.REPLACE : FileConflictPolicy.FAIL;
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return MediaType.parseMediaType(contentType);
    }
}
