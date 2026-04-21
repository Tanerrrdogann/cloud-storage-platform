package com.authsystem.authservice.consumer;

import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.consumer.exception.TemporaryConsumerException;
import com.authsystem.authservice.dto.BaseEvent;
import com.authsystem.authservice.entity.ProcessedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

import java.io.IOException;

@Slf4j
public abstract class AbstractEventConsumer<T extends BaseEvent> {

    private static final int MAX_RETRY_COUNT = 3;

    private final MessageProcessingService messageProcessingService;
    private final AuthMetricsService authMetricsService;
    private final String consumerName;
    private final String actionPrefix;
    private final String retryRoutingKey;
    private final int retryTtlMs;

    protected AbstractEventConsumer(
            MessageProcessingService messageProcessingService,
            AuthMetricsService authMetricsService,
            String consumerName,
            String actionPrefix,
            String retryRoutingKey,
            int retryTtlMs
    ) {
        this.messageProcessingService = messageProcessingService;
        this.authMetricsService = authMetricsService;
        this.consumerName = consumerName;
        this.actionPrefix = actionPrefix;
        this.retryRoutingKey = retryRoutingKey;
        this.retryTtlMs = retryTtlMs;
    }

    protected void handle(T message, Message rawMessage, Channel channel) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        ProcessedMessage processedMessage = null;

        try {
            String idempotencyKey = messageProcessingService.requireIdempotencyKey(message);

            if (messageProcessingService.isAlreadyProcessed(idempotencyKey)) {
                authMetricsService.recordDuplicateSkipped(message.getEventType(), consumerName);

                log.warn(
                        "ST_LOG | ACTION:{}_DUPLICATE_SKIPPED | EVENT_ID:{} | IDEMPOTENCY_KEY:{} | CORRELATION_ID:{}",
                        actionPrefix,
                        message.getEventId(),
                        idempotencyKey,
                        message.getCorrelationId()
                );

                channel.basicAck(deliveryTag, false);
                return;
            }

            processedMessage = messageProcessingService.createOrMarkProcessing(
                    message,
                    idempotencyKey,
                    consumerName
            );

            log.info(
                    "ST_LOG | ACTION:{}_CONSUME_START | EVENT_ID:{} | EVENT_TYPE:{} | IDEMPOTENCY_KEY:{} | AGGREGATE_ID:{} | CORRELATION_ID:{}",
                    actionPrefix,
                    message.getEventId(),
                    message.getEventType(),
                    idempotencyKey,
                    message.getAggregateId(),
                    message.getCorrelationId()
            );

            validate(message);
            process(message);

            messageProcessingService.markProcessed(processedMessage);
            authMetricsService.recordConsumerProcessed(message.getEventType(), consumerName);

            channel.basicAck(deliveryTag, false);

            log.info(
                    "ST_LOG | ACTION:{}_CONSUME_ACK | EVENT_ID:{} | IDEMPOTENCY_KEY:{}",
                    actionPrefix,
                    message.getEventId(),
                    idempotencyKey
            );

        } catch (TemporaryConsumerException ex) {
            messageProcessingService.markFailed(processedMessage);
            int retryCount = messageProcessingService.getRetryCount(rawMessage);

            if (retryCount >= MAX_RETRY_COUNT) {
                authMetricsService.recordConsumerDlq(message.getEventType(), consumerName, "retry_exhausted");

                log.error(
                        "ST_LOG | ACTION:{}_RETRY_EXHAUSTED | EVENT_ID:{} | RETRY_COUNT:{} | REASON:{}",
                        actionPrefix,
                        message.getEventId(),
                        retryCount,
                        ex.getMessage()
                );

                channel.basicReject(deliveryTag, false);
                return;
            }

            int nextRetryCount = retryCount + 1;

            messageProcessingService.sendToRetry(
                    message,
                    retryRoutingKey,
                    nextRetryCount
            );
            authMetricsService.recordConsumerRetry(message.getEventType(), consumerName);

            channel.basicAck(deliveryTag, false);

            log.warn(
                    "ST_LOG | ACTION:{}_RETRY_SCHEDULED | EVENT_ID:{} | RETRY_COUNT:{} | DELAY_MS:{} | REASON:{}",
                    actionPrefix,
                    message.getEventId(),
                    nextRetryCount,
                    retryTtlMs,
                    ex.getMessage()
            );

        } catch (PermanentConsumerException ex) {
            messageProcessingService.markFailed(processedMessage);
            authMetricsService.recordConsumerDlq(message.getEventType(), consumerName, "permanent");

            log.error(
                    "ST_LOG | ACTION:{}_DLQ | EVENT_ID:{} | REASON:{}",
                    actionPrefix,
                    message.getEventId(),
                    ex.getMessage()
            );

            channel.basicReject(deliveryTag, false);

        } catch (Exception ex) {
            messageProcessingService.markFailed(processedMessage);
            authMetricsService.recordConsumerDlq(message.getEventType(), consumerName, "unknown");

            log.error(
                    "ST_LOG | ACTION:{}_UNKNOWN_ERROR | EVENT_ID:{} | REASON:{}",
                    actionPrefix,
                    message.getEventId(),
                    ex.getMessage()
            );

            channel.basicReject(deliveryTag, false);
        }
    }

    protected abstract void validate(T message);

    protected abstract void process(T message);
}
