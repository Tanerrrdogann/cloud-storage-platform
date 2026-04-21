package com.authsystem.authservice.service;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.dto.BaseEvent;
import com.authsystem.authservice.dto.ForgotPasswordMessage;
import com.authsystem.authservice.dto.PasswordResetMessage;
import com.authsystem.authservice.dto.UserCreatedMessage;
import com.authsystem.authservice.dto.UserDeletedMessage;
import com.authsystem.authservice.dto.UserLockedMessage;
import com.authsystem.authservice.dto.UserLoggedInMessage;
import com.authsystem.authservice.dto.UserLoggedOutMessage;
import com.authsystem.authservice.dto.UserRoleUpdatedMessage;
import com.authsystem.authservice.dto.UserSuspendedMessage;
import com.authsystem.authservice.dto.UserVerifiedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;
    private final AuthMetricsService authMetricsService;

    public void sendUserCreatedMessage(UserCreatedMessage message) {
        publish(message, RabbitMQConfig.USER_CREATED_ROUTING_KEY);
    }

    public void sendUserDeletedMessage(UserDeletedMessage message) {
        publish(message, RabbitMQConfig.USER_DELETED_ROUTING_KEY);
    }

    public void sendRoleUpdatedMessage(UserRoleUpdatedMessage message) {
        publish(message, RabbitMQConfig.USER_ROLE_UPDATED_ROUTING_KEY);
    }

    public void sendUserSuspendedMessage(UserSuspendedMessage message) {
        publish(message, RabbitMQConfig.USER_SUSPENDED_ROUTING_KEY);
    }

    public void sendUserVerifiedMessage(UserVerifiedMessage message) {
        publish(message, RabbitMQConfig.USER_VERIFIED_ROUTING_KEY);
    }

    public void sendUserLoggedInMessage(UserLoggedInMessage message) {
        publish(message, RabbitMQConfig.USER_LOGGED_IN_ROUTING_KEY);
    }

    public void sendUserLoggedOutMessage(UserLoggedOutMessage message) {
        publish(message, RabbitMQConfig.USER_LOGGED_OUT_ROUTING_KEY);
    }

    public void sendForgotPasswordMessage(ForgotPasswordMessage message) {
        publish(message, RabbitMQConfig.FORGOT_PASSWORD_ROUTING_KEY);
    }

    public void sendPasswordResetMessage(PasswordResetMessage message) {
        publish(message, RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY);
    }

    public void sendUserLockedMessage(UserLockedMessage message) {
        publish(message, RabbitMQConfig.USER_LOCKED_ROUTING_KEY);
    }

    private void publish(BaseEvent message, String routingKey) {
        log.info(
                "ST_LOG | ACTION:RABBITMQ_SEND_START | EVENT_TYPE:{} | EVENT_ID:{} | TARGET_EXCHANGE:{} | ROUTING_KEY:{} | AGGREGATE_ID:{} | CORRELATION_ID:{}",
                message.getEventType(),
                message.getEventId(),
                RabbitMQConfig.EXCHANGE_AUTH,
                routingKey,
                message.getAggregateId(),
                message.getCorrelationId()
        );

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_AUTH, routingKey, message);
            authMetricsService.recordEventPublishSuccess(message.getEventType());

            log.info(
                    "ST_LOG | ACTION:RABBITMQ_SEND_SUCCESS | EVENT_TYPE:{} | EVENT_ID:{} | ROUTING_KEY:{}",
                    message.getEventType(),
                    message.getEventId(),
                    routingKey
            );
        } catch (Exception ex) {
            authMetricsService.recordEventPublishFailure(message.getEventType());

            log.error(
                    "ST_LOG | TYPE:SYSTEM_ERROR | ACTION:RABBITMQ_SEND_FAILED | EVENT_TYPE:{} | EVENT_ID:{} | ROUTING_KEY:{} | ERROR:{}",
                    message.getEventType(),
                    message.getEventId(),
                    routingKey,
                    ex.getMessage()
            );
        }
    }
}
