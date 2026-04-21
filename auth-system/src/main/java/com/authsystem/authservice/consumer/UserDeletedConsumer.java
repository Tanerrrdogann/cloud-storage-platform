package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserDeletedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class UserDeletedConsumer extends AbstractEventConsumer<UserDeletedMessage> {

    public UserDeletedConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-deleted-consumer",
                "USER_DELETED",
                RabbitMQConfig.USER_DELETED_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_DELETED_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.USER_DELETED_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserDeletedMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserDeletedMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.deleted payload: userId is required.");
        }

        if (message.getEmail() == null || message.getEmail().isBlank()) {
            throw new PermanentConsumerException("Invalid user.deleted payload: email is required.");
        }
    }

    @Override
    protected void process(UserDeletedMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_DELETED_PROCESSED | USER_ID:{} | EMAIL:{}",
                message.getUserId(),
                message.getEmail()
        );
    }
}
