package com.authsystem.authservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_AUTH = "auth.exchange";
    public static final String EXCHANGE_AUTH_DLX = "auth.dlx";
    public static final String EXCHANGE_AUTH_RETRY = "auth.retry.exchange";

    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String USER_CREATED_DLQ = "user.created.dlq";
    public static final String USER_CREATED_RETRY_QUEUE = "user.created.retry.queue";
    public static final String USER_CREATED_ROUTING_KEY = "user.created.key";
    public static final String USER_CREATED_DLX_ROUTING_KEY = "user.created.dlq.key";
    public static final String USER_CREATED_RETRY_ROUTING_KEY = "user.created.retry.key";
    public static final int USER_CREATED_RETRY_TTL_MS = 5000;

    public static final String USER_DELETED_QUEUE = "user.deleted.queue";
    public static final String USER_DELETED_DLQ = "user.deleted.dlq";
    public static final String USER_DELETED_RETRY_QUEUE = "user.deleted.retry.queue";
    public static final String USER_DELETED_ROUTING_KEY = "user.deleted.key";
    public static final String USER_DELETED_DLX_ROUTING_KEY = "user.deleted.dlq.key";
    public static final String USER_DELETED_RETRY_ROUTING_KEY = "user.deleted.retry.key";
    public static final int USER_DELETED_RETRY_TTL_MS = 5000;

    public static final String USER_ROLE_UPDATED_QUEUE = "user.role.updated.queue";
    public static final String USER_ROLE_UPDATED_DLQ = "user.role.updated.dlq";
    public static final String USER_ROLE_UPDATED_RETRY_QUEUE = "user.role.updated.retry.queue";
    public static final String USER_ROLE_UPDATED_ROUTING_KEY = "user.role.updated.key";
    public static final String USER_ROLE_UPDATED_DLX_ROUTING_KEY = "user.role.updated.dlq.key";
    public static final String USER_ROLE_UPDATED_RETRY_ROUTING_KEY = "user.role.updated.retry.key";
    public static final int USER_ROLE_UPDATED_RETRY_TTL_MS = 5000;

    public static final String USER_SUSPENDED_QUEUE = "user.suspended.queue";
    public static final String USER_SUSPENDED_DLQ = "user.suspended.dlq";
    public static final String USER_SUSPENDED_RETRY_QUEUE = "user.suspended.retry.queue";
    public static final String USER_SUSPENDED_ROUTING_KEY = "user.suspended.key";
    public static final String USER_SUSPENDED_DLX_ROUTING_KEY = "user.suspended.dlq.key";
    public static final String USER_SUSPENDED_RETRY_ROUTING_KEY = "user.suspended.retry.key";
    public static final int USER_SUSPENDED_RETRY_TTL_MS = 5000;

    public static final String USER_VERIFIED_QUEUE = "user.verified.queue";
    public static final String USER_VERIFIED_DLQ = "user.verified.dlq";
    public static final String USER_VERIFIED_RETRY_QUEUE = "user.verified.retry.queue";
    public static final String USER_VERIFIED_ROUTING_KEY = "user.verified.key";
    public static final String USER_VERIFIED_DLX_ROUTING_KEY = "user.verified.dlq.key";
    public static final String USER_VERIFIED_RETRY_ROUTING_KEY = "user.verified.retry.key";
    public static final int USER_VERIFIED_RETRY_TTL_MS = 5000;

    public static final String USER_LOGGED_IN_QUEUE = "user.loggedin.queue";
    public static final String USER_LOGGED_IN_DLQ = "user.loggedin.dlq";
    public static final String USER_LOGGED_IN_RETRY_QUEUE = "user.loggedin.retry.queue";
    public static final String USER_LOGGED_IN_ROUTING_KEY = "user.loggedin.key";
    public static final String USER_LOGGED_IN_DLX_ROUTING_KEY = "user.loggedin.dlq.key";
    public static final String USER_LOGGED_IN_RETRY_ROUTING_KEY = "user.loggedin.retry.key";
    public static final int USER_LOGGED_IN_RETRY_TTL_MS = 5000;

    public static final String USER_LOGGED_OUT_QUEUE = "user.loggedout.queue";
    public static final String USER_LOGGED_OUT_DLQ = "user.loggedout.dlq";
    public static final String USER_LOGGED_OUT_RETRY_QUEUE = "user.loggedout.retry.queue";
    public static final String USER_LOGGED_OUT_ROUTING_KEY = "user.loggedout.key";
    public static final String USER_LOGGED_OUT_DLX_ROUTING_KEY = "user.loggedout.dlq.key";
    public static final String USER_LOGGED_OUT_RETRY_ROUTING_KEY = "user.loggedout.retry.key";
    public static final int USER_LOGGED_OUT_RETRY_TTL_MS = 5000;

    public static final String FORGOT_PASSWORD_QUEUE = "user.forgotpassword.queue";
    public static final String FORGOT_PASSWORD_DLQ = "user.forgotpassword.dlq";
    public static final String FORGOT_PASSWORD_RETRY_QUEUE = "user.forgotpassword.retry.queue";
    public static final String FORGOT_PASSWORD_ROUTING_KEY = "user.forgotpassword.key";
    public static final String FORGOT_PASSWORD_DLX_ROUTING_KEY = "user.forgotpassword.dlq.key";
    public static final String FORGOT_PASSWORD_RETRY_ROUTING_KEY = "user.forgotpassword.retry.key";
    public static final int FORGOT_PASSWORD_RETRY_TTL_MS = 5000;

    public static final String PASSWORD_RESET_QUEUE = "user.passwordreset.queue";
    public static final String PASSWORD_RESET_DLQ = "user.passwordreset.dlq";
    public static final String PASSWORD_RESET_RETRY_QUEUE = "user.passwordreset.retry.queue";
    public static final String PASSWORD_RESET_ROUTING_KEY = "user.passwordreset.key";
    public static final String PASSWORD_RESET_DLX_ROUTING_KEY = "user.passwordreset.dlq.key";
    public static final String PASSWORD_RESET_RETRY_ROUTING_KEY = "user.passwordreset.retry.key";
    public static final int PASSWORD_RESET_RETRY_TTL_MS = 5000;

    public static final String USER_LOCKED_QUEUE = "user.locked.queue";
    public static final String USER_LOCKED_DLQ = "user.locked.dlq";
    public static final String USER_LOCKED_RETRY_QUEUE = "user.locked.retry.queue";
    public static final String USER_LOCKED_ROUTING_KEY = "user.locked.key";
    public static final String USER_LOCKED_DLX_ROUTING_KEY = "user.locked.dlq.key";
    public static final String USER_LOCKED_RETRY_ROUTING_KEY = "user.locked.retry.key";
    public static final int USER_LOCKED_RETRY_TTL_MS = 5000;

    @Bean
    public SimpleRabbitListenerContainerFactory manualAckListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(10);
        return factory;
    }

    @Bean
    public DirectExchange authExchange() {
        log.info("ST_LOG | ACTION:RABBITMQ_INIT | TYPE:EXCHANGE | NAME:{}", EXCHANGE_AUTH);
        return new DirectExchange(EXCHANGE_AUTH);
    }

    @Bean
    public DirectExchange authDeadLetterExchange() {
        log.info("ST_LOG | ACTION:RABBITMQ_INIT | TYPE:EXCHANGE | NAME:{}", EXCHANGE_AUTH_DLX);
        return new DirectExchange(EXCHANGE_AUTH_DLX);
    }

    @Bean
    public DirectExchange authRetryExchange() {
        log.info("ST_LOG | ACTION:RABBITMQ_INIT | TYPE:EXCHANGE | NAME:{}", EXCHANGE_AUTH_RETRY);
        return new DirectExchange(EXCHANGE_AUTH_RETRY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        log.info("ST_LOG | ACTION:RABBITMQ_INIT | TYPE:CONVERTER | FORMAT:JSON");
        return new Jackson2JsonMessageConverter();
    }

    @Bean public Queue userCreatedQueue() { return mainQueue(USER_CREATED_QUEUE, USER_CREATED_DLX_ROUTING_KEY); }
    @Bean public Queue userCreatedDeadLetterQueue() { return dlq(USER_CREATED_DLQ); }
    @Bean public Queue userCreatedRetryQueue() { return retryQueue(USER_CREATED_RETRY_QUEUE, USER_CREATED_RETRY_TTL_MS, USER_CREATED_ROUTING_KEY); }

    @Bean public Queue userDeletedQueue() { return mainQueue(USER_DELETED_QUEUE, USER_DELETED_DLX_ROUTING_KEY); }
    @Bean public Queue userDeletedDeadLetterQueue() { return dlq(USER_DELETED_DLQ); }
    @Bean public Queue userDeletedRetryQueue() { return retryQueue(USER_DELETED_RETRY_QUEUE, USER_DELETED_RETRY_TTL_MS, USER_DELETED_ROUTING_KEY); }

    @Bean public Queue userRoleUpdatedQueue() { return mainQueue(USER_ROLE_UPDATED_QUEUE, USER_ROLE_UPDATED_DLX_ROUTING_KEY); }
    @Bean public Queue userRoleUpdatedDeadLetterQueue() { return dlq(USER_ROLE_UPDATED_DLQ); }
    @Bean public Queue userRoleUpdatedRetryQueue() { return retryQueue(USER_ROLE_UPDATED_RETRY_QUEUE, USER_ROLE_UPDATED_RETRY_TTL_MS, USER_ROLE_UPDATED_ROUTING_KEY); }

    @Bean public Queue userSuspendedQueue() { return mainQueue(USER_SUSPENDED_QUEUE, USER_SUSPENDED_DLX_ROUTING_KEY); }
    @Bean public Queue userSuspendedDeadLetterQueue() { return dlq(USER_SUSPENDED_DLQ); }
    @Bean public Queue userSuspendedRetryQueue() { return retryQueue(USER_SUSPENDED_RETRY_QUEUE, USER_SUSPENDED_RETRY_TTL_MS, USER_SUSPENDED_ROUTING_KEY); }

    @Bean public Queue userVerifiedQueue() { return mainQueue(USER_VERIFIED_QUEUE, USER_VERIFIED_DLX_ROUTING_KEY); }
    @Bean public Queue userVerifiedDeadLetterQueue() { return dlq(USER_VERIFIED_DLQ); }
    @Bean public Queue userVerifiedRetryQueue() { return retryQueue(USER_VERIFIED_RETRY_QUEUE, USER_VERIFIED_RETRY_TTL_MS, USER_VERIFIED_ROUTING_KEY); }

    @Bean public Queue userLoggedInQueue() { return mainQueue(USER_LOGGED_IN_QUEUE, USER_LOGGED_IN_DLX_ROUTING_KEY); }
    @Bean public Queue userLoggedInDeadLetterQueue() { return dlq(USER_LOGGED_IN_DLQ); }
    @Bean public Queue userLoggedInRetryQueue() { return retryQueue(USER_LOGGED_IN_RETRY_QUEUE, USER_LOGGED_IN_RETRY_TTL_MS, USER_LOGGED_IN_ROUTING_KEY); }

    @Bean public Queue userLoggedOutQueue() { return mainQueue(USER_LOGGED_OUT_QUEUE, USER_LOGGED_OUT_DLX_ROUTING_KEY); }
    @Bean public Queue userLoggedOutDeadLetterQueue() { return dlq(USER_LOGGED_OUT_DLQ); }
    @Bean public Queue userLoggedOutRetryQueue() { return retryQueue(USER_LOGGED_OUT_RETRY_QUEUE, USER_LOGGED_OUT_RETRY_TTL_MS, USER_LOGGED_OUT_ROUTING_KEY); }

    @Bean public Queue forgotPasswordQueue() { return mainQueue(FORGOT_PASSWORD_QUEUE, FORGOT_PASSWORD_DLX_ROUTING_KEY); }
    @Bean public Queue forgotPasswordDeadLetterQueue() { return dlq(FORGOT_PASSWORD_DLQ); }
    @Bean public Queue forgotPasswordRetryQueue() { return retryQueue(FORGOT_PASSWORD_RETRY_QUEUE, FORGOT_PASSWORD_RETRY_TTL_MS, FORGOT_PASSWORD_ROUTING_KEY); }

    @Bean public Queue passwordResetQueue() { return mainQueue(PASSWORD_RESET_QUEUE, PASSWORD_RESET_DLX_ROUTING_KEY); }
    @Bean public Queue passwordResetDeadLetterQueue() { return dlq(PASSWORD_RESET_DLQ); }
    @Bean public Queue passwordResetRetryQueue() { return retryQueue(PASSWORD_RESET_RETRY_QUEUE, PASSWORD_RESET_RETRY_TTL_MS, PASSWORD_RESET_ROUTING_KEY); }

    @Bean public Queue userLockedQueue() { return mainQueue(USER_LOCKED_QUEUE, USER_LOCKED_DLX_ROUTING_KEY); }
    @Bean public Queue userLockedDeadLetterQueue() { return dlq(USER_LOCKED_DLQ); }
    @Bean public Queue userLockedRetryQueue() { return retryQueue(USER_LOCKED_RETRY_QUEUE, USER_LOCKED_RETRY_TTL_MS, USER_LOCKED_ROUTING_KEY); }

    @Bean public Binding userCreatedBinding(@Qualifier("userCreatedQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_CREATED_ROUTING_KEY); }
    @Bean public Binding userCreatedDeadLetterBinding(@Qualifier("userCreatedDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_CREATED_DLX_ROUTING_KEY); }
    @Bean public Binding userCreatedRetryBinding(@Qualifier("userCreatedRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_CREATED_RETRY_ROUTING_KEY); }

    @Bean public Binding userDeletedBinding(@Qualifier("userDeletedQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_DELETED_ROUTING_KEY); }
    @Bean public Binding userDeletedDeadLetterBinding(@Qualifier("userDeletedDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_DELETED_DLX_ROUTING_KEY); }
    @Bean public Binding userDeletedRetryBinding(@Qualifier("userDeletedRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_DELETED_RETRY_ROUTING_KEY); }

    @Bean public Binding userRoleUpdatedBinding(@Qualifier("userRoleUpdatedQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_ROLE_UPDATED_ROUTING_KEY); }
    @Bean public Binding userRoleUpdatedDeadLetterBinding(@Qualifier("userRoleUpdatedDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_ROLE_UPDATED_DLX_ROUTING_KEY); }
    @Bean public Binding userRoleUpdatedRetryBinding(@Qualifier("userRoleUpdatedRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_ROLE_UPDATED_RETRY_ROUTING_KEY); }

    @Bean public Binding userSuspendedBinding(@Qualifier("userSuspendedQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_SUSPENDED_ROUTING_KEY); }
    @Bean public Binding userSuspendedDeadLetterBinding(@Qualifier("userSuspendedDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_SUSPENDED_DLX_ROUTING_KEY); }
    @Bean public Binding userSuspendedRetryBinding(@Qualifier("userSuspendedRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_SUSPENDED_RETRY_ROUTING_KEY); }

    @Bean public Binding userVerifiedBinding(@Qualifier("userVerifiedQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_VERIFIED_ROUTING_KEY); }
    @Bean public Binding userVerifiedDeadLetterBinding(@Qualifier("userVerifiedDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_VERIFIED_DLX_ROUTING_KEY); }
    @Bean public Binding userVerifiedRetryBinding(@Qualifier("userVerifiedRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_VERIFIED_RETRY_ROUTING_KEY); }

    @Bean public Binding userLoggedInBinding(@Qualifier("userLoggedInQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOGGED_IN_ROUTING_KEY); }
    @Bean public Binding userLoggedInDeadLetterBinding(@Qualifier("userLoggedInDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOGGED_IN_DLX_ROUTING_KEY); }
    @Bean public Binding userLoggedInRetryBinding(@Qualifier("userLoggedInRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOGGED_IN_RETRY_ROUTING_KEY); }

    @Bean public Binding userLoggedOutBinding(@Qualifier("userLoggedOutQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOGGED_OUT_ROUTING_KEY); }
    @Bean public Binding userLoggedOutDeadLetterBinding(@Qualifier("userLoggedOutDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOGGED_OUT_DLX_ROUTING_KEY); }
    @Bean public Binding userLoggedOutRetryBinding(@Qualifier("userLoggedOutRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOGGED_OUT_RETRY_ROUTING_KEY); }

    @Bean public Binding forgotPasswordBinding(@Qualifier("forgotPasswordQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, FORGOT_PASSWORD_ROUTING_KEY); }
    @Bean public Binding forgotPasswordDeadLetterBinding(@Qualifier("forgotPasswordDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, FORGOT_PASSWORD_DLX_ROUTING_KEY); }
    @Bean public Binding forgotPasswordRetryBinding(@Qualifier("forgotPasswordRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, FORGOT_PASSWORD_RETRY_ROUTING_KEY); }

    @Bean public Binding passwordResetBinding(@Qualifier("passwordResetQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, PASSWORD_RESET_ROUTING_KEY); }
    @Bean public Binding passwordResetDeadLetterBinding(@Qualifier("passwordResetDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, PASSWORD_RESET_DLX_ROUTING_KEY); }
    @Bean public Binding passwordResetRetryBinding(@Qualifier("passwordResetRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, PASSWORD_RESET_RETRY_ROUTING_KEY); }

    @Bean public Binding userLockedBinding(@Qualifier("userLockedQueue") Queue queue, @Qualifier("authExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOCKED_ROUTING_KEY); }
    @Bean public Binding userLockedDeadLetterBinding(@Qualifier("userLockedDeadLetterQueue") Queue queue, @Qualifier("authDeadLetterExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOCKED_DLX_ROUTING_KEY); }
    @Bean public Binding userLockedRetryBinding(@Qualifier("userLockedRetryQueue") Queue queue, @Qualifier("authRetryExchange") DirectExchange exchange) { return mainBinding(queue, exchange, USER_LOCKED_RETRY_ROUTING_KEY); }

    private Queue mainQueue(String queueName, String deadLetterRoutingKey) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(EXCHANGE_AUTH_DLX)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    private Queue dlq(String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    private Queue retryQueue(String queueName, int ttlMs, String mainRoutingKey) {
        return QueueBuilder.durable(queueName)
                .ttl(ttlMs)
                .deadLetterExchange(EXCHANGE_AUTH)
                .deadLetterRoutingKey(mainRoutingKey)
                .build();
    }

    private Binding mainBinding(Queue queue, DirectExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
