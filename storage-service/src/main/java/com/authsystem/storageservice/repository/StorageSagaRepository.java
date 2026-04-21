package com.authsystem.storageservice.repository;

import com.authsystem.storageservice.entity.StorageSaga;
import com.authsystem.storageservice.entity.enums.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StorageSagaRepository extends JpaRepository<StorageSaga, UUID> {
    List<StorageSaga> findByAggregateId(String aggregateId);
    List<StorageSaga> findByStatus(SagaStatus status);
}
