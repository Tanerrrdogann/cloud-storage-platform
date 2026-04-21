package com.authsystem.storageservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class StorageRabbitMQConfig {

    public static final String STORAGE_EXCHANGE = "storage.exchange";
    public static final String STORAGE_DLX = "storage.dlx";
    public static final String STORAGE_RETRY_EXCHANGE = "storage.retry.exchange";

    public static final String OBJECT_REPLICATION_QUEUE = "storage.object.replication.queue";
    public static final String OBJECT_REPLICATION_RETRY_QUEUE = "storage.object.replication.retry.queue";
    public static final String OBJECT_REPLICATION_DLQ = "storage.object.replication.dlq";

    public static final String OBJECT_REPLICATION_ROUTING_KEY = "storage.object.replication";
    public static final String OBJECT_REPLICATION_RETRY_ROUTING_KEY = "storage.object.replication.retry";
    public static final String OBJECT_REPLICATION_DLX_ROUTING_KEY = "storage.object.replication.dlq";

    public static final int OBJECT_REPLICATION_RETRY_TTL_MS = 5000;

    @Bean
    public DirectExchange storageExchange() {
        return ExchangeBuilder.directExchange(STORAGE_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange storageDeadLetterExchange() {
        return ExchangeBuilder.directExchange(STORAGE_DLX)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange storageRetryExchange() {
        return ExchangeBuilder.directExchange(STORAGE_RETRY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue objectReplicationQueue() {
        return QueueBuilder.durable(OBJECT_REPLICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", STORAGE_DLX)
                .withArgument("x-dead-letter-routing-key", OBJECT_REPLICATION_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue objectReplicationRetryQueue() {
        return QueueBuilder.durable(OBJECT_REPLICATION_RETRY_QUEUE)
                .withArgument("x-message-ttl", OBJECT_REPLICATION_RETRY_TTL_MS)
                .withArgument("x-dead-letter-exchange", STORAGE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OBJECT_REPLICATION_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue objectReplicationDlq() {
        return QueueBuilder.durable(OBJECT_REPLICATION_DLQ).build();
    }

    @Bean
    public Binding objectReplicationBinding(
            Queue objectReplicationQueue,
            DirectExchange storageExchange
    ) {
        return BindingBuilder.bind(objectReplicationQueue)
                .to(storageExchange)
                .with(OBJECT_REPLICATION_ROUTING_KEY);
    }

    @Bean
    public Binding objectReplicationRetryBinding(
            Queue objectReplicationRetryQueue,
            DirectExchange storageRetryExchange
    ) {
        return BindingBuilder.bind(objectReplicationRetryQueue)
                .to(storageRetryExchange)
                .with(OBJECT_REPLICATION_RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding objectReplicationDlqBinding(
            Queue objectReplicationDlq,
            DirectExchange storageDeadLetterExchange
    ) {
        return BindingBuilder.bind(objectReplicationDlq)
                .to(storageDeadLetterExchange)
                .with(OBJECT_REPLICATION_DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter storageMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate storageRabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter storageMessageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(storageMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory storageManualAckListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter storageMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(storageMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        return factory;
    }
}
