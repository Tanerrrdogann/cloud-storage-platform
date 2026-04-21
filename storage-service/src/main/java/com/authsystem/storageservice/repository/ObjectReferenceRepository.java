package com.authsystem.storageservice.repository;

import com.authsystem.storageservice.entity.ObjectReference;
import com.authsystem.storageservice.entity.enums.ObjectReferenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObjectReferenceRepository extends JpaRepository<ObjectReference, UUID> {
    List<ObjectReference> findByBlobId(UUID blobId);
    long countByBlobId(UUID blobId);
    Optional<ObjectReference> findByObject_Id(UUID objectId);
    long countByBlob_IdAndStatus(UUID blobId, ObjectReferenceStatus status);
}
