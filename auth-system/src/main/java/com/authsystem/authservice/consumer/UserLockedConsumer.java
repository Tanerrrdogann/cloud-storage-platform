package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserLockedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class UserLockedConsumer extends AbstractEventConsumer<UserLockedMessage> {

    public UserLockedConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-locked-consumer",
                "USER_LOCKED",
                RabbitMQConfig.USER_LOCKED_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_LOCKED_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.USER_LOCKED_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserLockedMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserLockedMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.locked payload: userId is required.");
        }

        if (message.getEmail() == null || message.getEmail().isBlank()) {
            throw new PermanentConsumerException("Invalid user.locked payload: email is required.");
        }
    }

    @Override
    protected void process(UserLockedMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_LOCKED_PROCESSED | USER_ID:{} | EMAIL:{} | IP_ADDRESS:{}",
                message.getUserId(),
                message.getEmail(),
                message.getIpAddress()
        );
    }
}
