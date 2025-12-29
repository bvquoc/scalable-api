package com.project.domain.service;

import com.project.domain.model.Order;
import com.project.infrastructure.persistence.entity.OrderEntity;
import com.project.infrastructure.persistence.mapper.OrderMapper;
import com.project.infrastructure.persistence.repository.OrderRepository;
import com.project.messaging.dto.OrderEvent;
import com.project.messaging.dto.OrderProcessingMessage;
import com.project.messaging.producer.KafkaProducer;
import com.project.messaging.producer.RabbitMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Order domain operations.
 * Orchestrates order processing with RabbitMQ tasks and Kafka events.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final RabbitMQProducer rabbitMQProducer;
    private final KafkaProducer kafkaProducer;

    public OrderService(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            RabbitMQProducer rabbitMQProducer,
            KafkaProducer kafkaProducer) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.rabbitMQProducer = rabbitMQProducer;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * Create new order.
     * Publishes order event to Kafka and sends processing task to RabbitMQ.
     */
    public Order createOrder(Order order) {
        log.info("Creating order: userId={}, amount={}", order.getUserId(), order.getTotalAmount());

        // Generate unique order number
        String orderNumber = generateOrderNumber();
        order.setOrderNumber(orderNumber);

        // Save order to database
        OrderEntity entity = orderMapper.toEntity(order);
        OrderEntity saved = orderRepository.save(entity);

        Order createdOrder = orderMapper.toDomain(saved);

        // Publish order created event to Kafka
        publishOrderEvent(createdOrder, "CREATED");

        // Send order processing task to RabbitMQ
        sendOrderProcessingTask(createdOrder);

        log.info("Order created successfully: id={}, orderNumber={}", saved.getId(), orderNumber);

        return createdOrder;
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);

        return orderRepository.findById(id)
                .map(orderMapper::toDomain);
    }

    /**
     * Get order by order number.
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        log.debug("Fetching order by orderNumber: {}", orderNumber);

        return orderRepository.findByOrderNumber(orderNumber)
                .map(orderMapper::toDomain);
    }

    /**
     * Get orders by user (paginated).
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        log.debug("Fetching orders for user: userId={}", userId);

        return orderRepository.findByUserId(userId, pageable)
                .map(orderMapper::toDomain);
    }

    /**
     * Get orders by status.
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        log.debug("Fetching orders by status: {}", status);

        OrderEntity.OrderStatus entityStatus = OrderEntity.OrderStatus.valueOf(status.name());
        return orderRepository.findByStatus(entityStatus).stream()
                .map(orderMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get recent orders (last 7 days).
     */
    @Transactional(readOnly = true)
    public List<Order> getRecentOrders() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        log.debug("Fetching recent orders since: {}", since);

        return orderRepository.findRecentOrders(since).stream()
                .map(orderMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Update order status.
     * Publishes status change event to Kafka.
     */
    public Order updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        log.info("Updating order status: id={}, newStatus={}", id, newStatus);

        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        OrderEntity.OrderStatus entityStatus = OrderEntity.OrderStatus.valueOf(newStatus.name());
        entity.setStatus(entityStatus);
        OrderEntity updated = orderRepository.save(entity);

        Order updatedOrder = orderMapper.toDomain(updated);

        // Publish status change event
        publishOrderEvent(updatedOrder, newStatus.name());

        log.info("Order status updated: id={}, orderNumber={}, status={}",
            id, updatedOrder.getOrderNumber(), newStatus);

        return updatedOrder;
    }

    /**
     * Cancel order.
     */
    public Order cancelOrder(Long id) {
        return updateOrderStatus(id, Order.OrderStatus.CANCELLED);
    }

    /**
     * Delete order.
     */
    public void deleteOrder(Long id) {
        log.info("Deleting order: id={}", id);

        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found: " + id);
        }

        orderRepository.deleteById(id);

        log.info("Order deleted successfully: id={}", id);
    }

    /**
     * Generate unique order number.
     */
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Publish order event to Kafka.
     */
    private void publishOrderEvent(Order order, String eventType) {
        OrderEvent event = new OrderEvent(
            eventType,
            order.getId(),
            order.getOrderNumber(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getStatus().name()
        );

        kafkaProducer.publishOrderEvent(event);
    }

    /**
     * Send order processing task to RabbitMQ.
     */
    private void sendOrderProcessingTask(Order order) {
        OrderProcessingMessage message = new OrderProcessingMessage(
            order.getId(),
            order.getOrderNumber(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getShippingAddress(),
            order.getCreatedAt()
        );

        rabbitMQProducer.sendOrderProcessingTask(message);
    }
}
