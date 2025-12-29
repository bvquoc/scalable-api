package com.project.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for task queue messaging.
 *
 * Use Cases:
 * - Order processing (async order fulfillment)
 * - Email notifications (async email sending)
 * - Report generation (background processing)
 * - Data export tasks
 *
 * Pattern: Work Queue (competing consumers)
 * - Multiple workers consume from same queue
 * - Round-robin distribution
 * - Auto-acknowledgment on successful processing
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Queue names
    public static final String ORDER_PROCESSING_QUEUE = "order.processing";
    public static final String EMAIL_NOTIFICATION_QUEUE = "email.notification";
    public static final String REPORT_GENERATION_QUEUE = "report.generation";

    // Exchange names
    public static final String TASKS_EXCHANGE = "tasks.exchange";

    // Routing keys
    public static final String ORDER_PROCESSING_KEY = "task.order.processing";
    public static final String EMAIL_NOTIFICATION_KEY = "task.email.notification";
    public static final String REPORT_GENERATION_KEY = "task.report.generation";

    /**
     * Message converter using JSON serialization.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate for sending messages.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * Listener container factory with JSON converter.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }

    /**
     * Direct exchange for task routing.
     */
    @Bean
    public DirectExchange tasksExchange() {
        return new DirectExchange(TASKS_EXCHANGE, true, false);
    }

    /**
     * Order processing queue.
     */
    @Bean
    public Queue orderProcessingQueue() {
        return QueueBuilder.durable(ORDER_PROCESSING_QUEUE)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    /**
     * Email notification queue.
     */
    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable(EMAIL_NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Report generation queue.
     */
    @Bean
    public Queue reportGenerationQueue() {
        return QueueBuilder.durable(REPORT_GENERATION_QUEUE)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Bind order processing queue to exchange.
     */
    @Bean
    public Binding orderProcessingBinding(Queue orderProcessingQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(orderProcessingQueue)
                .to(tasksExchange)
                .with(ORDER_PROCESSING_KEY);
    }

    /**
     * Bind email notification queue to exchange.
     */
    @Bean
    public Binding emailNotificationBinding(Queue emailNotificationQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(emailNotificationQueue)
                .to(tasksExchange)
                .with(EMAIL_NOTIFICATION_KEY);
    }

    /**
     * Bind report generation queue to exchange.
     */
    @Bean
    public Binding reportGenerationBinding(Queue reportGenerationQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(reportGenerationQueue)
                .to(tasksExchange)
                .with(REPORT_GENERATION_KEY);
    }
}
