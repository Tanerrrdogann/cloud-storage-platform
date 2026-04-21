package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserCreatedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
public class UserCreatedConsumer extends AbstractEventConsumer<UserCreatedMessage> {

    public UserCreatedConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-created-consumer",
                "USER_CREATED",
                RabbitMQConfig.USER_CREATED_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_CREATED_RETRY_TTL_MS
        );
    }

    @Transactional
    @RabbitListener(
            queues = RabbitMQConfig.USER_CREATED_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserCreatedMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserCreatedMessage message) {
        if (message.getUserId() == null || message.getEmail() == null || message.getEmail().isBlank()) {
            throw new PermanentConsumerException("Invalid user.created payload.");
        }
    }

    @Override
    protected void process(UserCreatedMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_CREATED_PROCESSED | USER_ID:{} | EMAIL:{}",
                message.getUserId(),
                message.getEmail()
        );
    }
}
