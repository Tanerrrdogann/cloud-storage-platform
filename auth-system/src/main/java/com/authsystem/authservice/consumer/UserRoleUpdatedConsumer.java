package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserRoleUpdatedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class UserRoleUpdatedConsumer extends AbstractEventConsumer<UserRoleUpdatedMessage> {

    public UserRoleUpdatedConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-role-updated-consumer",
                "USER_ROLE_UPDATED",
                RabbitMQConfig.USER_ROLE_UPDATED_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_ROLE_UPDATED_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.USER_ROLE_UPDATED_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserRoleUpdatedMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserRoleUpdatedMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.role.updated payload: userId is required.");
        }

        if (message.getNewRole() == null || message.getNewRole().isBlank()) {
            throw new PermanentConsumerException("Invalid user.role.updated payload: newRole is required.");
        }
    }

    @Override
    protected void process(UserRoleUpdatedMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_ROLE_UPDATED_PROCESSED | USER_ID:{} | NEW_ROLE:{}",
                message.getUserId(),
                message.getNewRole()
        );
    }
}
