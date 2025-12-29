package com.project.messaging.consumer;

import com.project.config.RabbitMQConfig;
import com.project.messaging.dto.OrderProcessingMessage;
import com.project.messaging.producer.RabbitMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ consumer for processing task queue messages.
 * Multiple instances can consume from the same queue (competing consumers).
 */
@Service
public class RabbitMQConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumer.class);

    /**
     * Process order fulfillment tasks.
     * Simulates async order processing workflow.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PROCESSING_QUEUE)
    public void processOrderTask(OrderProcessingMessage message) {
        try {
            log.info("Processing order task: orderId={}, orderNumber={}, amount={}",
                message.getOrderId(), message.getOrderNumber(), message.getTotalAmount());

            // Simulate order processing steps
            // 1. Validate payment
            log.debug("Validating payment for order: {}", message.getOrderNumber());
            Thread.sleep(500);

            // 2. Reserve inventory
            log.debug("Reserving inventory for order: {}", message.getOrderNumber());
            Thread.sleep(500);

            // 3. Generate shipping label
            log.debug("Generating shipping label for order: {}", message.getOrderNumber());
            Thread.sleep(300);

            // 4. Notify warehouse
            log.debug("Notifying warehouse for order: {}", message.getOrderNumber());
            Thread.sleep(200);

            log.info("Successfully processed order task: orderId={}", message.getOrderId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Order processing interrupted: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process order task: {}", e.getMessage(), e);
            throw new RuntimeException("Order processing failed", e);
        }
    }

    /**
     * Process email notification tasks.
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_NOTIFICATION_QUEUE)
    public void processEmailNotificationTask(RabbitMQProducer.EmailNotificationMessage message) {
        try {
            log.info("Processing email notification: recipient={}, subject={}",
                message.getRecipient(), message.getSubject());

            // Simulate email sending
            Thread.sleep(1000);

            log.info("Successfully sent email to: {}", message.getRecipient());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email notification interrupted: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Process report generation tasks.
     */
    @RabbitListener(queues = RabbitMQConfig.REPORT_GENERATION_QUEUE)
    public void processReportGenerationTask(RabbitMQProducer.ReportGenerationMessage message) {
        try {
            log.info("Processing report generation: reportType={}, userId={}",
                message.getReportType(), message.getUserId());

            // Simulate report generation
            Thread.sleep(2000);

            log.info("Successfully generated report: reportType={}", message.getReportType());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Report generation interrupted: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate report: {}", e.getMessage(), e);
        }
    }
}
