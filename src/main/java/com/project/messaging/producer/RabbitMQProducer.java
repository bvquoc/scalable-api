package com.project.messaging.producer;

import com.project.config.RabbitMQConfig;
import com.project.messaging.dto.OrderProcessingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ producer for task queue messages.
 * Sends tasks to queues for async processing by workers.
 */
@Service
public class RabbitMQProducer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Send order processing task to queue.
     *
     * @param message Order processing message
     */
    public void sendOrderProcessingTask(OrderProcessingMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASKS_EXCHANGE,
                RabbitMQConfig.ORDER_PROCESSING_KEY,
                message
            );

            log.info("Sent order processing task to queue: orderId={}, orderNumber={}",
                message.getOrderId(), message.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send order processing task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send order processing task", e);
        }
    }

    /**
     * Send email notification task to queue.
     *
     * @param recipient Email recipient
     * @param subject Email subject
     * @param body Email body
     */
    public void sendEmailNotificationTask(String recipient, String subject, String body) {
        try {
            EmailNotificationMessage message = new EmailNotificationMessage(recipient, subject, body);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASKS_EXCHANGE,
                RabbitMQConfig.EMAIL_NOTIFICATION_KEY,
                message
            );

            log.info("Sent email notification task to queue: recipient={}", recipient);

        } catch (Exception e) {
            log.error("Failed to send email notification task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email notification task", e);
        }
    }

    /**
     * Send report generation task to queue.
     *
     * @param reportType Type of report
     * @param userId User requesting the report
     * @param parameters Report parameters
     */
    public void sendReportGenerationTask(String reportType, Long userId, String parameters) {
        try {
            ReportGenerationMessage message = new ReportGenerationMessage(reportType, userId, parameters);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASKS_EXCHANGE,
                RabbitMQConfig.REPORT_GENERATION_KEY,
                message
            );

            log.info("Sent report generation task to queue: reportType={}, userId={}", reportType, userId);

        } catch (Exception e) {
            log.error("Failed to send report generation task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send report generation task", e);
        }
    }

    // Helper DTOs
    public static class EmailNotificationMessage {
        private String recipient;
        private String subject;
        private String body;

        public EmailNotificationMessage() {}

        public EmailNotificationMessage(String recipient, String subject, String body) {
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
        }

        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    public static class ReportGenerationMessage {
        private String reportType;
        private Long userId;
        private String parameters;

        public ReportGenerationMessage() {}

        public ReportGenerationMessage(String reportType, Long userId, String parameters) {
            this.reportType = reportType;
            this.userId = userId;
            this.parameters = parameters;
        }

        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
    }
}
