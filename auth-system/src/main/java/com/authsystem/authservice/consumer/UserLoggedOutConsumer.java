package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserLoggedOutMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class UserLoggedOutConsumer extends AbstractEventConsumer<UserLoggedOutMessage> {

    public UserLoggedOutConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-logged-out-consumer",
                "USER_LOGGED_OUT",
                RabbitMQConfig.USER_LOGGED_OUT_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_LOGGED_OUT_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.USER_LOGGED_OUT_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserLoggedOutMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserLoggedOutMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.loggedout payload: userId is required.");
        }
    }

    @Override
    protected void process(UserLoggedOutMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_LOGGED_OUT_PROCESSED | USER_ID:{} | EMAIL:{}",
                message.getUserId(),
                message.getEmail()
        );
    }
}
