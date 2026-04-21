package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.ForgotPasswordMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ForgotPasswordConsumer extends AbstractEventConsumer<ForgotPasswordMessage> {

    public ForgotPasswordConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "forgot-password-consumer",
                "FORGOT_PASSWORD",
                RabbitMQConfig.FORGOT_PASSWORD_RETRY_ROUTING_KEY,
                RabbitMQConfig.FORGOT_PASSWORD_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.FORGOT_PASSWORD_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(ForgotPasswordMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(ForgotPasswordMessage message) {
        if (message.getEmail() == null || message.getEmail().isBlank()) {
            throw new PermanentConsumerException("Invalid user.forgotpassword payload: email is required.");
        }

        if (message.getResetToken() == null || message.getResetToken().isBlank()) {
            throw new PermanentConsumerException("Invalid user.forgotpassword payload: resetToken is required.");
        }
    }

    @Override
    protected void process(ForgotPasswordMessage message) {
        log.info(
                "ST_LOG | ACTION:FORGOT_PASSWORD_PROCESSED | EMAIL:{}",
                message.getEmail()
        );
    }
}
