package com.authsystem.storageservice.repository;

import com.authsystem.storageservice.entity.ReplicationTask;
import com.authsystem.storageservice.entity.enums.ReplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReplicationTaskRepository extends JpaRepository<ReplicationTask, UUID> {
    List<ReplicationTask> findByStatus(ReplicationStatus status);
    List<ReplicationTask> findByStatusAndNextRetryAtBefore(ReplicationStatus status, Instant nextRetryAt);
}
