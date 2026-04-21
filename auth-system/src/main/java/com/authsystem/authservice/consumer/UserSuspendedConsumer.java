package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserSuspendedMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class UserSuspendedConsumer extends AbstractEventConsumer<UserSuspendedMessage> {

    public UserSuspendedConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-suspended-consumer",
                "USER_SUSPENDED",
                RabbitMQConfig.USER_SUSPENDED_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_SUSPENDED_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.USER_SUSPENDED_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserSuspendedMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserSuspendedMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.suspended payload: userId is required.");
        }

        if (message.getSuspendedDays() <= 0) {
            throw new PermanentConsumerException("Invalid user.suspended payload: suspendedDays must be positive.");
        }
    }

    @Override
    protected void process(UserSuspendedMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_SUSPENDED_PROCESSED | USER_ID:{} | DAYS:{} | SUSPENDED_BY:{}",
                message.getUserId(),
                message.getSuspendedDays(),
                message.getSuspendedBy()
        );
    }
}
