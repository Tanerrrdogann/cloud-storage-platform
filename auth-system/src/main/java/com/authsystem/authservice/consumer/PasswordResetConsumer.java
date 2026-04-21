package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.PasswordResetMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PasswordResetConsumer extends AbstractEventConsumer<PasswordResetMessage> {

    public PasswordResetConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "password-reset-consumer",
                "PASSWORD_RESET",
                RabbitMQConfig.PASSWORD_RESET_RETRY_ROUTING_KEY,
                RabbitMQConfig.PASSWORD_RESET_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.PASSWORD_RESET_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(PasswordResetMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(PasswordResetMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.passwordreset payload: userId is required.");
        }

        if (message.getEmail() == null || message.getEmail().isBlank()) {
            throw new PermanentConsumerException("Invalid user.passwordreset payload: email is required.");
        }
    }

    @Override
    protected void process(PasswordResetMessage message) {
        log.info(
                "ST_LOG | ACTION:PASSWORD_RESET_PROCESSED | USER_ID:{} | EMAIL:{}",
                message.getUserId(),
                message.getEmail()
        );
    }
}
