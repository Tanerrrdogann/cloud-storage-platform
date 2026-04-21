package com.authsystem.storageservice.repository;

import com.authsystem.storageservice.entity.StoredObject;
import com.authsystem.storageservice.entity.enums.StoredObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredObjectRepository extends JpaRepository<StoredObject, UUID> {
    Optional<StoredObject> findByOwnerIdAndObjectKey(String ownerId, String objectKey);
    Optional<StoredObject> findByOwnerIdAndObjectKeyAndStatus(String ownerId, String objectKey, StoredObjectStatus status);
    List<StoredObject> findByOwnerId(String ownerId);
    List<StoredObject> findByOwnerIdAndObjectKeyStartingWith(String ownerId, String objectKeyPrefix);
    List<StoredObject> findByOwnerIdAndObjectKeyStartingWithAndStatus(String ownerId, String objectKeyPrefix, StoredObjectStatus status);
    List<StoredObject> findByStatus(StoredObjectStatus status);
    boolean existsByOwnerIdAndObjectKeyAndStatus(String ownerId, String objectKey, StoredObjectStatus status);
}
