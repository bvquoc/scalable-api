package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.OrderEntity;
import com.project.infrastructure.persistence.entity.ProductEntity;
import com.project.infrastructure.persistence.entity.UserEntity;
import com.project.infrastructure.persistence.entity.OrderItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrderRepository.
 */
class OrderRepositoryIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private UserEntity testUser;
    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setEmail("order@example.com");
        testUser.setUsername("orderuser");
        testUser.setStatus(UserEntity.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        testProduct = new ProductEntity();
        testProduct.setName("Test Product");
        testProduct.setSku("TEST-PROD-001");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStockQuantity(100);
        testProduct.setCategory("Test");
        testProduct.setIsActive(true);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void shouldSaveAndFindOrderByOrderNumber() {
        // Given
        OrderEntity order = new OrderEntity();
        order.setUser(testUser);
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderEntity.OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setShippingAddress("123 Test St, Test City");

        // When
        OrderEntity saved = orderRepository.save(order);
        Optional<OrderEntity> found = orderRepository.findByOrderNumber("ORD-001");

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo("ORD-001");
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("199.98"));
    }

    @Test
    void shouldFindOrdersByUserId() {
        // Given
        OrderEntity order1 = createOrder("ORD-USER-001", OrderEntity.OrderStatus.PENDING);
        OrderEntity order2 = createOrder("ORD-USER-002", OrderEntity.OrderStatus.PROCESSING);

        orderRepository.save(order1);
        orderRepository.save(order2);

        // When
        Page<OrderEntity> orders = orderRepository.findByUserId(testUser.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(orders.getContent()).hasSize(2);
        assertThat(orders.getContent()).allMatch(o -> o.getUser().getId().equals(testUser.getId()));
    }

    @Test
    void shouldFindOrdersByStatus() {
        // Given
        OrderEntity pendingOrder = createOrder("ORD-PEND-001", OrderEntity.OrderStatus.PENDING);
        OrderEntity shippedOrder = createOrder("ORD-SHIP-001", OrderEntity.OrderStatus.SHIPPED);

        orderRepository.save(pendingOrder);
        orderRepository.save(shippedOrder);

        // When
        List<OrderEntity> pendingOrders = orderRepository.findByStatus(OrderEntity.OrderStatus.PENDING);
        List<OrderEntity> shippedOrders = orderRepository.findByStatus(OrderEntity.OrderStatus.SHIPPED);

        // Then
        assertThat(pendingOrders).isNotEmpty();
        assertThat(pendingOrders).allMatch(o -> o.getStatus() == OrderEntity.OrderStatus.PENDING);
        assertThat(shippedOrders).isNotEmpty();
        assertThat(shippedOrders).allMatch(o -> o.getStatus() == OrderEntity.OrderStatus.SHIPPED);
    }

    @Test
    void shouldFindOrdersByUserIdAndStatus() {
        // Given
        OrderEntity pendingOrder = createOrder("ORD-US-001", OrderEntity.OrderStatus.PENDING);
        OrderEntity processingOrder = createOrder("ORD-US-002", OrderEntity.OrderStatus.PROCESSING);

        orderRepository.save(pendingOrder);
        orderRepository.save(processingOrder);

        // When
        List<OrderEntity> pending = orderRepository.findByUserIdAndStatus(
            testUser.getId(),
            OrderEntity.OrderStatus.PENDING
        );

        // Then
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getOrderNumber()).isEqualTo("ORD-US-001");
    }

    @Test
    void shouldFindRecentOrders() {
        // Given
        OrderEntity recentOrder = createOrder("ORD-REC-001", OrderEntity.OrderStatus.PENDING);
        orderRepository.save(recentOrder);

        LocalDateTime since = LocalDateTime.now().minusHours(1);

        // When
        List<OrderEntity> recentOrders = orderRepository.findRecentOrders(since);

        // Then
        assertThat(recentOrders).isNotEmpty();
        assertThat(recentOrders).allMatch(o -> o.getCreatedAt().isAfter(since));
    }

    @Test
    void shouldFindStalePendingOrders() {
        // Given - manually set old timestamp
        OrderEntity staleOrder = createOrder("ORD-STALE-001", OrderEntity.OrderStatus.PENDING);
        OrderEntity saved = orderRepository.save(staleOrder);

        // Simulate old order by checking with future threshold
        LocalDateTime futureThreshold = LocalDateTime.now().plusDays(1);

        // When
        List<OrderEntity> staleOrders = orderRepository.findStalePendingOrders(futureThreshold);

        // Then
        assertThat(staleOrders).isNotEmpty();
    }

    @Test
    void shouldCountOrdersByUserId() {
        // Given
        orderRepository.save(createOrder("ORD-CNT-001", OrderEntity.OrderStatus.PENDING));
        orderRepository.save(createOrder("ORD-CNT-002", OrderEntity.OrderStatus.PROCESSING));
        orderRepository.save(createOrder("ORD-CNT-003", OrderEntity.OrderStatus.SHIPPED));

        // When
        long count = orderRepository.countByUserId(testUser.getId());

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldCheckOrderNumberExists() {
        // Given
        OrderEntity order = createOrder("ORD-EXISTS-001", OrderEntity.OrderStatus.PENDING);
        orderRepository.save(order);

        // When
        boolean exists = orderRepository.existsByOrderNumber("ORD-EXISTS-001");
        boolean notExists = orderRepository.existsByOrderNumber("ORD-NOTEXISTS-001");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldHandleOrderWithItems() {
        // Given
        OrderEntity order = new OrderEntity();
        order.setUser(testUser);
        order.setOrderNumber("ORD-ITEMS-001");
        order.setStatus(OrderEntity.OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setShippingAddress("123 Test St");

        OrderItemEntity item = new OrderItemEntity();
        item.setProduct(testProduct);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("99.99"));

        order.addItem(item);

        // When
        OrderEntity saved = orderRepository.save(order);
        orderRepository.flush();

        Optional<OrderEntity> found = orderRepository.findByOrderNumber("ORD-ITEMS-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getQuantity()).isEqualTo(2);
    }

    private OrderEntity createOrder(String orderNumber, OrderEntity.OrderStatus status) {
        OrderEntity order = new OrderEntity();
        order.setUser(testUser);
        order.setOrderNumber(orderNumber);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setShippingAddress("Test Address");
        return order;
    }
}
