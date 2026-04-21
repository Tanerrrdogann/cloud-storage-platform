package com.authsystem.storageservice.repository;

import com.authsystem.storageservice.entity.StorageWal;
import com.authsystem.storageservice.entity.enums.WalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageWalRepository extends JpaRepository<StorageWal, UUID> {
    Optional<StorageWal> findByOperationId(String operationId);
    List<StorageWal> findByStatusIn(Collection<WalStatus> statuses);
}
