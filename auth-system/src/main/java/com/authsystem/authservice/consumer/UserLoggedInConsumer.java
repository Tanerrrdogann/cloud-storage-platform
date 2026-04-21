package com.authsystem.authservice.consumer;

import com.authsystem.authservice.config.RabbitMQConfig;
import com.authsystem.authservice.consumer.exception.PermanentConsumerException;
import com.authsystem.authservice.dto.UserLoggedInMessage;
import com.authsystem.authservice.metrics.AuthMetricsService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class UserLoggedInConsumer extends AbstractEventConsumer<UserLoggedInMessage> {

    public UserLoggedInConsumer(MessageProcessingService messageProcessingService, AuthMetricsService authMetricsService) {
        super(
                messageProcessingService,
                authMetricsService,
                "user-logged-in-consumer",
                "USER_LOGGED_IN",
                RabbitMQConfig.USER_LOGGED_IN_RETRY_ROUTING_KEY,
                RabbitMQConfig.USER_LOGGED_IN_RETRY_TTL_MS
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.USER_LOGGED_IN_QUEUE,
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void consume(UserLoggedInMessage message, Message rawMessage, Channel channel) throws IOException {
        handle(message, rawMessage, channel);
    }

    @Override
    protected void validate(UserLoggedInMessage message) {
        if (message.getUserId() == null) {
            throw new PermanentConsumerException("Invalid user.loggedin payload: userId is required.");
        }
    }

    @Override
    protected void process(UserLoggedInMessage message) {
        log.info(
                "ST_LOG | ACTION:USER_LOGGED_IN_PROCESSED | USER_ID:{} | IP_ADDRESS:{}",
                message.getUserId(),
                message.getIpAddress()
        );
    }
}
