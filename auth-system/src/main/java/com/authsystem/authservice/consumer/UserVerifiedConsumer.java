package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserVerifiedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class UserVerifiedConsumer extends AbstractEventConsumer<UserVerifiedMessage> {

    public UserVerifiedConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-verified-consumer",
                "USER_VERIFIED",
                RabbitMQConfig.USER_VERIFIED_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_VERIFIED_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.USER_VERIFIED_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserVerifiedMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserVerifiedMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.verified payload: userId is required.");
        }

        if (message.getEmail() == null || message.getEmail().isBlank()) {
            throw new PermanentConsumerException("Invalid user.verified payload: email is required.");
        }
    }

    @Override
    protected void process(UserVerifiedMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_VERIFIED_PROCESSED | USER_ID:{} | EMAIL:{}",
                message.getUserId(),
                message.getEmail()
        );
    }
}
