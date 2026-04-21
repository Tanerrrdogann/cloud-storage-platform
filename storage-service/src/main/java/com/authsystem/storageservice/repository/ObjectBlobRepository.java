package com.authsystem.storageservice.repository;

import com.authsystem.storageservice.entity.ObjectBlob;
import com.authsystem.storageservice.entity.enums.BlobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObjectBlobRepository extends JpaRepository<ObjectBlob, UUID> {
    Optional<ObjectBlob> findByContentHash(String contentHash);
    List<ObjectBlob> findByStatus(BlobStatus status);
}
