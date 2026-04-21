package com.authsystem.storageservice.consumer;

import com.authsystem.storageservice.config.StorageRabbitMQConfig;
import com.authsystem.storageservice.dto.ObjectReplicationMessage;
import com.authsystem.storageservice.entity.ReplicationTask;
import com.authsystem.storageservice.metrics.StorageMetricsService;
import com.authsystem.storageservice.repository.ReplicationTaskRepository;
import com.authsystem.storageservice.service.ReplicationTaskService;
import com.authsystem.storageservice.service.S3ReplicationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplicationConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final ReplicationTaskRepository replicationTaskRepository;
    private final ReplicationTaskService replicationTaskService;
    private final RabbitTemplate storageRabbitTemplate;
    private final S3ReplicationService s3ReplicationService;
    private final StorageMetricsService storageMetricsService;

    @RabbitListener(
            queues = StorageRabbitMQConfig.OBJECT_REPLICATION_QUEUE,
            containerFactory = "storageManualAckListenerContainerFactory"
    )
    public void consume(
            ObjectReplicationMessage message,
            Message rawMessage,
            Channel channel
    ) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();

        try {
            validate(message);

            ReplicationTask task = replicationTaskRepository.findById(message.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Replication task not found"));

            processReplication(message);

            replicationTaskService.markCompleted(task);
            storageMetricsService.recordReplicationSuccess();
            channel.basicAck(deliveryTag, false);

            log.info(
                    "ST_LOG | ACTION:OBJECT_REPLICATION_CONSUMED | EVENT_ID:{} | TASK_ID:{} | OBJECT_ID:{} | STATUS:COMPLETED",
                    message.getEventId(),
                    message.getTaskId(),
                    message.getObjectId()
            );
        } catch (IllegalArgumentException ex) {
            markTaskFailedIfPossible(message, ex.getMessage());
            storageMetricsService.recordReplicationFailure("permanent");

            log.error(
                    "ST_LOG | ACTION:OBJECT_REPLICATION_PERMANENT_FAILURE | EVENT_ID:{} | MSG:{}",
                    safeEventId(message),
                    ex.getMessage()
            );

            channel.basicReject(deliveryTag, false);
        } catch (Exception ex) {
            int retryCount = getRetryCount(rawMessage);

            if (retryCount >= MAX_RETRY_COUNT) {
                markTaskFailedIfPossible(message, ex.getMessage());
                storageMetricsService.recordReplicationFailure("retry_exhausted");

                log.error(
                        "ST_LOG | ACTION:OBJECT_REPLICATION_RETRY_EXHAUSTED | EVENT_ID:{} | RETRY_COUNT:{} | MSG:{}",
                        safeEventId(message),
                        retryCount,
                        ex.getMessage()
                );

                channel.basicReject(deliveryTag, false);
                return;
            }

            markTaskRetryIfPossible(message, ex.getMessage());
            sendToRetry(message, retryCount + 1);
            channel.basicAck(deliveryTag, false);

            log.warn(
                    "ST_LOG | ACTION:OBJECT_REPLICATION_SENT_TO_RETRY | EVENT_ID:{} | RETRY_COUNT:{} | MSG:{}",
                    safeEventId(message),
                    retryCount + 1,
                    ex.getMessage()
            );
        }
    }

    private void validate(ObjectReplicationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message is required");
        }

        if (message.getTaskId() == null) {
            throw new IllegalArgumentException("taskId is required");
        }

        if (message.getObjectId() == null) {
            throw new IllegalArgumentException("objectId is required");
        }

        if (message.getBlobId() == null) {
            throw new IllegalArgumentException("blobId is required");
        }

        if (message.getStoragePath() == null || message.getStoragePath().isBlank()) {
            throw new IllegalArgumentException("storagePath is required");
        }
    }

    private void processReplication(ObjectReplicationMessage message) {
        s3ReplicationService.replicate(message);

        log.info(
                "ST_LOG | ACTION:OBJECT_REPLICATION_PROCESSING_COMPLETED | TASK_ID:{} | STORAGE_PATH:{} | TARGET:{}",
                message.getTaskId(),
                message.getStoragePath(),
                message.getTarget()
        );
    }

    private int getRetryCount(Message rawMessage) {
        Object retryCount = rawMessage.getMessageProperties().getHeaders().get("x-retry-count");

        if (retryCount instanceof Integer value) {
            return value;
        }

        if (retryCount instanceof String value) {
            return Integer.parseInt(value);
        }

        return 0;
    }

    private void sendToRetry(ObjectReplicationMessage message, int retryCount) {
        storageRabbitTemplate.convertAndSend(
                StorageRabbitMQConfig.STORAGE_RETRY_EXCHANGE,
                StorageRabbitMQConfig.OBJECT_REPLICATION_RETRY_ROUTING_KEY,
                message,
                rabbitMessage -> {
                    rabbitMessage.getMessageProperties().setHeader("x-retry-count", retryCount);
                    return rabbitMessage;
                }
        );
    }

    private void markTaskRetryIfPossible(ObjectReplicationMessage message, String errorMessage) {
        if (message == null || message.getTaskId() == null) {
            return;
        }

        replicationTaskRepository.findById(message.getTaskId())
                .ifPresent(task -> replicationTaskService.markRetryPending(task, errorMessage));
    }

    private void markTaskFailedIfPossible(ObjectReplicationMessage message, String errorMessage) {
        if (message == null || message.getTaskId() == null) {
            return;
        }

        replicationTaskRepository.findById(message.getTaskId())
                .ifPresent(task -> replicationTaskService.markFailed(task, errorMessage));
    }

    private String safeEventId(ObjectReplicationMessage message) {
        if (message == null || message.getEventId() == null) {
            return "UNKNOWN";
        }

        return message.getEventId();
    }
}
