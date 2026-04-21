package com.authsystem.storageservice.service;

import com.authsystem.storageservice.config.StorageRabbitMQConfig;
import com.authsystem.storageservice.dto.ObjectReplicationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplicationProducer {

    private final RabbitTemplate storageRabbitTemplate;

    public void publishObjectReplication(
            UUID taskId,
            UUID objectId,
            UUID blobId,
            String ownerId,
            String objectKey,
            String storagePath,
            String target
    ) {
        ObjectReplicationMessage message = ObjectReplicationMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("object.replication.requested")
                .occurredAt(Instant.now())
                .producer("storage-service")
                .aggregateId(objectId.toString())
                .idempotencyKey("object.replication:" + taskId)
                .correlationId(MDC.get("correlationId"))
                .payloadVersion(1)
                .taskId(taskId)
                .objectId(objectId)
                .blobId(blobId)
                .ownerId(ownerId)
                .objectKey(objectKey)
                .storagePath(storagePath)
                .target(target)
                .build();

        storageRabbitTemplate.convertAndSend(
                StorageRabbitMQConfig.STORAGE_EXCHANGE,
                StorageRabbitMQConfig.OBJECT_REPLICATION_ROUTING_KEY,
                message
        );

        log.info(
                "ST_LOG | ACTION:OBJECT_REPLICATION_EVENT_PUBLISHED | EVENT_ID:{} | TASK_ID:{} | OBJECT_ID:{} | TARGET:{}",
                message.getEventId(),
                taskId,
                objectId,
                target
        );
    }
}
