package com.authsystem.storageservice.service;

import com.authsystem.storageservice.entity.ObjectBlob;
import com.authsystem.storageservice.entity.ReplicationTask;
import com.authsystem.storageservice.entity.StoredObject;
import com.authsystem.storageservice.entity.enums.ReplicationStatus;
import com.authsystem.storageservice.repository.ReplicationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReplicationTaskService {

    private final ReplicationTaskRepository replicationTaskRepository;

    @Transactional
    public ReplicationTask createPendingTask(
            StoredObject object,
            ObjectBlob blob,
            String target
    ) {
        ReplicationTask task = ReplicationTask.builder()
                .object(object)
                .blob(blob)
                .target(target)
                .status(ReplicationStatus.PENDING)
                .retryCount(0)
                .build();

        return replicationTaskRepository.save(task);
    }

    @Transactional
    public void markCompleted(ReplicationTask task) {
        task.setStatus(ReplicationStatus.COMPLETED);
        task.setLastError(null);
        task.setNextRetryAt(null);
        replicationTaskRepository.save(task);
    }

    @Transactional
    public void markRetryPending(ReplicationTask task, String errorMessage) {
        task.setStatus(ReplicationStatus.RETRY_PENDING);
        task.setRetryCount(task.getRetryCount() + 1);
        task.setLastError(errorMessage);
        task.setNextRetryAt(Instant.now().plusSeconds(5));
        replicationTaskRepository.save(task);
    }

    @Transactional
    public void markFailed(ReplicationTask task, String errorMessage) {
        task.setStatus(ReplicationStatus.FAILED);
        task.setLastError(errorMessage);
        task.setNextRetryAt(null);
        replicationTaskRepository.save(task);
    }
}
