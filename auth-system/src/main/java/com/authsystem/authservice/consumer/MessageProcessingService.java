package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.BaseEvent;
import com.authsystem.authservice.entity.ProcessedMessage;
import com.authsystem.authservice.repository.ProcessedMessageRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MessageProcessingService {

    public static final String RETRY_COUNT_HEADER = "x-retry-count";

    private final RabbitTemplate rabbitTemplate;
    private final ProcessedMessageRepository processedMessageRepository;

    public MessageProcessingService(
            RabbitTemplate rabbitTemplate,
            ProcessedMessageRepository processedMessageRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.processedMessageRepository = processedMessageRepository;
    }

    public String requireIdempotencyKey(BaseEvent event) {
        String idempotencyKey = event.getIdempotencyKey();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PermanentConsumerException("Missing idempotency key.");
        }

        return idempotencyKey;
    }

    public boolean isAlreadyProcessed(String idempotencyKey) {
        return processedMessageRepository.existsByIdempotencyKeyAndStatus(idempotencyKey, "PROCESSED");
    }

    public ProcessedMessage createOrMarkProcessing(BaseEvent event, String idempotencyKey, String consumerName) {
        return processedMessageRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    if (!"PROCESSED".equals(existing.getStatus())) {
                        existing.markProcessing();
                        return processedMessageRepository.save(existing);
                    }

                    return existing;
                })
                .orElseGet(() -> {
                    try {
                        return processedMessageRepository.save(new ProcessedMessage(
                                idempotencyKey,
                                event.getEventId(),
                                Instant.now(),
                                consumerName,
                                "PROCESSING"
                        ));
                    } catch (DataIntegrityViolationException ex) {
                        return processedMessageRepository.findByIdempotencyKey(idempotencyKey)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    public void markProcessed(ProcessedMessage processedMessage) {
        if (processedMessage == null) {
            return;
        }

        processedMessage.markProcessed();
        processedMessageRepository.save(processedMessage);
    }

    public void markFailed(ProcessedMessage processedMessage) {
        if (processedMessage == null) {
            return;
        }

        processedMessage.markFailed();
        processedMessageRepository.save(processedMessage);
    }

    public int getRetryCount(Message rawMessage) {
        Object retryCount = rawMessage.getMessageProperties().getHeaders().get(RETRY_COUNT_HEADER);

        if (retryCount instanceof Number number) {
            return number.intValue();
        }

        if (retryCount instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        return 0;
    }

    public void sendToRetry(BaseEvent event, String retryRoutingKey, int nextRetryCount) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_AUTH_RETRY,
                retryRoutingKey,
                event,
                retryMessage -> {
                    retryMessage.getMessageProperties().setHeader(RETRY_COUNT_HEADER, nextRetryCount);
                    return retryMessage;
                }
        );
    }
}
