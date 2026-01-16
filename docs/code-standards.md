# Code Standards & Guidelines - Scalable Spring Boot API

**Last Updated:** January 16, 2026
**Phase:** Phase 8: Demo Use Cases Completion
**Version:** 2.0

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Naming Conventions](#naming-conventions)
3. [Code Organization](#code-organization)
4. [Controller Patterns](#controller-patterns)
5. [Service Layer Patterns](#service-layer-patterns)
6. [Data Access Patterns](#data-access-patterns)
7. [Caching Patterns](#caching-patterns)
8. [Messaging Patterns](#messaging-patterns)
9. [Security Patterns](#security-patterns)
10. [Error Handling](#error-handling)
11. [Testing Standards](#testing-standards)
12. [Documentation Standards](#documentation-standards)

---

## Project Structure

### Directory Organization

```
src/main/java/com/project/
├── api/                      # REST Layer (Controllers, DTOs)
│   ├── controller/           # @RestController classes
│   ├── dto/                  # @Data DTOs (Request/Response)
│   ├── mapper/               # Entity ↔ DTO converters
│   └── exception/            # Exception handlers
│
├── domain/                   # Business Logic (DDD)
│   ├── model/               # Entity classes (JPA)
│   ├── service/             # @Service business logic
│   └── repository/          # Repository interfaces
│
├── infrastructure/          # Technical Implementation
│   ├── cache/              # Redis cache services
│   └── repository/         # JPA implementations
│
├── messaging/              # Event-Driven
│   ├── producer/           # Kafka/RabbitMQ publishers
│   ├── consumer/           # Kafka/RabbitMQ handlers
│   └── dto/               # Event DTOs (Avro, JSON)
│
├── security/              # Authentication & Authorization
│   ├── config/            # SecurityConfig
│   ├── authentication/    # Auth filters
│   ├── ratelimit/         # Rate limiting
│   └── handler/           # Custom security handlers
│
└── config/                # Spring Configuration
    ├── OpenApiConfig.java      # Swagger/OpenAPI
    ├── KafkaConfig.java        # Kafka setup
    ├── RabbitMQConfig.java     # RabbitMQ setup
    ├── RedisConfig.java        # Redis setup
    └── JpaConfig.java          # JPA/Hibernate setup

src/main/resources/
├── application.yml             # Base configuration
├── application-dev.yml         # Dev profile
├── application-prod.yml        # Prod profile
├── db/migration/              # Flyway SQL migrations
├── logback-spring.xml         # Logging configuration
└── schema.sql                 # DB schema (reference)

src/test/java/com/project/
├── domain/service/            # Service tests
├── api/controller/            # Controller integration tests
├── messaging/consumer/        # Consumer tests
└── fixtures/                  # Test data builders
```

---

## Naming Conventions

### Java Classes

| Type | Convention | Example |
|------|-----------|---------|
| Controller | `{Domain}Controller` | `OrderController`, `AnalyticsEventController` |
| Service | `{Domain}Service` | `OrderService`, `AnalyticsEventService` |
| Repository | `{Entity}Repository` | `OrderRepository`, `UserRepository` |
| Entity | `{Singular Noun}` | `Order`, `Product`, `User` |
| DTO (Request) | `Create{Entity}Request` or `{Entity}Request` | `CreateOrderRequest`, `AnalyticsEventRequest` |
| DTO (Response) | `{Entity}Response` | `OrderResponse`, `AnalyticsSummaryResponse` |
| Event | `{Entity}{Action}Event` | `OrderCreatedEvent`, `StatusChangedEvent` |
| Consumer | `{Domain}EventConsumer` | `OrderEventConsumer`, `AnalyticsEventConsumer` |
| Producer | `{Domain}Producer` or `KafkaProducer` | `KafkaProducer` |
| Filter | `{Purpose}Filter` | `ApiKeyAuthenticationFilter`, `RateLimitFilter` |
| Config | `{Component}Config` | `KafkaConfig`, `SecurityConfig` |
| Exception | `{Purpose}Exception` | `OrderNotFoundException`, `InvalidApiKeyException` |
| Test | `{Class}Test` | `OrderServiceTest`, `OrderControllerTest` |

### Variables & Methods

| Scope | Convention | Example |
|-------|-----------|---------|
| Package (static) | `UPPER_SNAKE_CASE` | `MAX_RETRIES`, `DEFAULT_TIMEOUT` |
| Instance field | `lowerCamelCase` | `orderId`, `totalAmount`, `createdAt` |
| Local variable | `lowerCamelCase` | `order`, `count`, `processedItems` |
| Parameter | `lowerCamelCase` | `orderId`, `filter`, `pageable` |
| Method name | `lowerCamelCase` | `getOrder()`, `createOrder()`, `updateStatus()` |
| Boolean method | `is{Property}()` or `has{Property}()` | `isActive()`, `hasPermission()` |
| Getter method | `get{Property}()` | `getOrderId()`, `getTotalAmount()` |
| Setter method | `set{Property}()` | `setStatus()` |
| Builder method | `with{Property}()` | `withStatus()`, `withAmount()` |

### Database Objects

| Object | Convention | Example |
|--------|-----------|---------|
| Table | `{entity_plural}` or `{entity}` | `users`, `orders`, `products` |
| Column | `snake_case` | `user_id`, `total_amount`, `created_at` |
| Primary Key | `id` | (always `id`) |
| Foreign Key | `{entity}_id` | `user_id`, `order_id` |
| Index | `idx_{table}_{column}` | `idx_orders_user_id` |
| Unique Key | `uk_{table}_{column}` | `uk_users_email` |

### API Endpoints

| HTTP Method | Convention | Example |
|-------------|-----------|---------|
| GET (list) | `/api/{resource}` | `GET /api/orders` |
| GET (single) | `/api/{resource}/{id}` | `GET /api/orders/1` |
| GET (alternative lookup) | `/api/{resource}/{field}/{value}` | `GET /api/orders/number/ORD-123` |
| GET (search) | `/api/{resource}/search` | `GET /api/products/search?q=mouse` |
| POST (create) | `POST /api/{resource}` | `POST /api/orders` |
| PATCH (update) | `PATCH /api/{resource}/{id}/{field}` | `PATCH /api/orders/1/status` |
| DELETE | `DELETE /api/{resource}/{id}` | `DELETE /api/orders/1` |

### Package Names

```
com.project.{layer}.{domain}

Examples:
  com.project.api.controller          # REST controllers
  com.project.api.dto                 # DTOs
  com.project.domain.service          # Business services
  com.project.domain.model            # Domain entities
  com.project.infrastructure.cache    # Cache services
  com.project.messaging.producer      # Kafka/RabbitMQ producers
  com.project.security.authentication # Auth filters
  com.project.config                  # Spring configuration
```

---

## Code Organization

### Class Structure Template

```java
@RestController
@RequestMapping("/api/{resource}")
@Tag(name = "Domain", description = "Domain operations")
public class DomainController {

    private final DomainService domainService;

    // Constructor injection (required fields only)
    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    // Public endpoints first
    @GetMapping
    public ResponseEntity<List<DomainResponse>> list() { ... }

    @GetMapping("/{id}")
    public ResponseEntity<DomainResponse> getById(@PathVariable Long id) { ... }

    @PostMapping
    public ResponseEntity<DomainResponse> create(@Valid @RequestBody CreateDomainRequest req) { ... }

    @PatchMapping("/{id}/field")
    public ResponseEntity<DomainResponse> updateField(@PathVariable Long id, ...) { ... }

    // Private helper methods at end
    private void validate(...) { ... }
}
```

### Layering Rules

#### Controller Layer

- **Responsibility:** HTTP request/response handling, validation, authentication
- **Rules:**
  - Accept only DTOs (never entities)
  - Return ResponseEntity (specify status code explicitly)
  - Call service layer directly
  - Minimal logic (use 3-line test: if you can't describe in 3 lines, move to service)
  - Use @Valid for input validation

#### Service Layer

- **Responsibility:** Business logic, orchestration, transactions
- **Rules:**
  - No HTTP knowledge (no servlet, request, response)
  - Work with entities internally, convert at boundaries
  - Single responsibility: one domain per service
  - Use @Transactional for DB operations
  - Call repository and other services
  - Publish events (Kafka) for async processing

#### Repository Layer

- **Responsibility:** Data access abstraction
- **Rules:**
  - Extend JpaRepository (Spring Data)
  - Define custom query methods (no logic)
  - Use @Query for complex queries only
  - No business logic in repositories

#### Infrastructure Layer

- **Responsibility:** Technical implementation (cache, messaging)
- **Rules:**
  - Encapsulate external systems (Redis, Kafka, RabbitMQ)
  - Expose clean interfaces to services
  - Handle errors and retries
  - No business logic

---

## Controller Patterns

### Standard REST Controller

```java
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management")
@SecurityRequirement(name = "apiKey")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Get paginated order list")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @ParameterObject Pageable pageable) {
        Page<Order> orders = orderService.listOrders(pageable);
        return ResponseEntity.ok(orders.map(this::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(toResponse(order));
    }

    @PostMapping
    @Operation(summary = "Create order")
    @ApiResponse(responseCode = "201", description = "Order created")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        Order order = orderService.updateStatus(id, status);
        return ResponseEntity.ok(toResponse(order));
    }

    // Mapper: Entity → DTO (hidden from clients)
    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
```

### Analytics Event Controller (NEW)

```java
@RestController
@RequestMapping("/api")
@Tag(name = "Analytics", description = "Analytics event logging")
@SecurityRequirement(name = "apiKey")
public class AnalyticsEventController {

    private final AnalyticsEventService analyticsEventService;

    public AnalyticsEventController(AnalyticsEventService analyticsEventService) {
        this.analyticsEventService = analyticsEventService;
    }

    @PostMapping("/events")
    @Operation(summary = "Log analytics event")
    @ApiResponse(responseCode = "202", description = "Event accepted for processing")
    public ResponseEntity<Void> logEvent(@Valid @RequestBody AnalyticsEventRequest request) {
        analyticsEventService.logEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/analytics/summary")
    @Operation(summary = "Get analytics summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        AnalyticsSummaryResponse summary = analyticsEventService.getSummary(date);
        return ResponseEntity.ok(summary);
    }
}
```

---

## Service Layer Patterns

### Standard Service with Caching

```java
@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaProducer kafkaProducer;
    private final CacheService cacheService;

    public OrderService(OrderRepository orderRepository,
                       KafkaProducer kafkaProducer,
                       CacheService cacheService) {
        this.orderRepository = orderRepository;
        this.kafkaProducer = kafkaProducer;
        this.cacheService = cacheService;
    }

    /**
     * Get order by ID with caching.
     * Cache-aside pattern: Check cache → Miss = DB query → Cache result
     */
    public Order getOrderById(Long id) {
        // Try cache first
        Order cached = cacheService.get("orders:" + id, Order.class);
        if (cached != null) {
            return cached;
        }

        // Cache miss: query database
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

        // Cache for future requests (30 minutes TTL)
        cacheService.set("orders:" + id, order, Duration.ofMinutes(30));

        return order;
    }

    /**
     * Create order with event publishing.
     * Side effects: DB write, Kafka publish, RabbitMQ send
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. Validate business rules
        if (request.getTotalAmount() <= 0) {
            throw new InvalidOrderException("Amount must be positive");
        }

        // 2. Create and persist entity
        Order order = new Order()
                .setUserId(request.getUserId())
                .setTotalAmount(request.getTotalAmount())
                .setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(order);
        log.info("Order created: orderId={}", saved.getId());

        // 3. Publish event (Kafka for fan-out)
        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getUserId(),
                saved.getCreatedAt()
        );
        kafkaProducer.sendOrderEvent(event);

        // 4. Send task (RabbitMQ for work queue)
        kafkaProducer.sendOrderProcessingMessage(saved.getId());

        return saved;
    }

    /**
     * Update order status with cache invalidation.
     */
    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = getOrderById(id);

        // Validate status transition
        if (!isValidTransition(order.getStatus(), newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from " + order.getStatus() + " to " + newStatus
            );
        }

        // Update entity
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        // Publish event
        StatusChangedEvent event = new StatusChangedEvent(
                order.getId(),
                order.getStatus(),
                newStatus
        );
        kafkaProducer.sendOrderEvent(event);

        // Invalidate cache for this order
        cacheService.delete("orders:" + id);

        return updated;
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        // Simple state machine validation
        return (from == OrderStatus.PENDING && to == OrderStatus.PROCESSING) ||
               (from == OrderStatus.PROCESSING && to == OrderStatus.SHIPPED) ||
               (from == OrderStatus.SHIPPED && to == OrderStatus.DELIVERED);
    }
}
```

### Analytics Event Service

```java
@Service
@Slf4j
public class AnalyticsEventService {

    private final KafkaProducer kafkaProducer;
    private final RedisTemplate<String, Long> redisTemplate;

    private static final List<String> EVENT_TYPES = List.of(
            "PAGE_VIEW", "BUTTON_CLICK", "FORM_SUBMIT", "API_CALL", "PURCHASE"
    );

    public AnalyticsEventService(KafkaProducer kafkaProducer,
                                 RedisTemplate<String, Long> redisTemplate) {
        this.kafkaProducer = kafkaProducer;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Log analytics event asynchronously.
     * Fire-and-forget pattern: Publish to Kafka and return immediately
     */
    public void logEvent(AnalyticsEventRequest request) {
        // Validate event type
        if (!EVENT_TYPES.contains(request.getEventType())) {
            throw new IllegalArgumentException(
                    "Invalid event type: " + request.getEventType()
            );
        }

        // Create event DTO
        AnalyticsEvent event = new AnalyticsEvent(
                request.getUserId(),
                request.getEventType(),
                request.getProperties()
        );

        // Publish to Kafka (non-blocking)
        kafkaProducer.sendAnalyticsEvent(event);
        log.debug("Analytics event published: type={}, userId={}",
                event.getEventType(), event.getUserId());
    }

    /**
     * Get analytics summary from Redis aggregation.
     */
    public AnalyticsSummaryResponse getSummary(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        Map<String, Long> eventCounts = new HashMap<>();

        // Query Redis for all event types
        for (String eventType : EVENT_TYPES) {
            Long count = getEventCount(eventType, date);
            eventCounts.put(eventType, count);
        }

        long totalEvents = eventCounts.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        return new AnalyticsSummaryResponse(
                date.toString(),
                eventCounts,
                totalEvents
        );
    }

    /**
     * Get event count for specific type and date.
     */
    private Long getEventCount(String eventType, LocalDate date) {
        String key = "analytics:events:" + eventType + ":" + date;
        Long count = redisTemplate.opsForValue().get(key);
        return count != null ? count : 0L;
    }
}
```

---

## Data Access Patterns

### Repository Definition

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Derived query methods (simple)
    Order findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    // Paginated query
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // Custom queries (complex)
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt >= :date")
    List<Order> findRecentOrdersByStatus(@Param("status") OrderStatus status,
                                         @Param("date") LocalDateTime date);
}
```

### Entity with Relationships

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_order_number", columnList = "order_number", unique = true)
})
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

---

## Caching Patterns

### Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.create(connectionFactory);
    }
}
```

### Cache Service Abstraction

```java
@Service
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) return null;
            return (T) value;
        } catch (Exception e) {
            log.warn("Cache get failed: key={}", key, e);
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Cache set failed: key={}", key, e);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Cache delete failed: key={}", key, e);
        }
    }
}
```

### Cache Invalidation Patterns

```java
// Pattern 1: Manual invalidation in service
@Transactional
public void updateProduct(Long id, UpdateProductRequest request) {
    Product product = productRepository.findById(id).orElseThrow();
    product.setName(request.getName());
    productRepository.save(product);

    // Invalidate cache
    cacheService.delete("products:" + id);
    cacheService.delete("product-list:*"); // Pattern delete if needed
}

// Pattern 2: Event-driven invalidation
@Component
@Slf4j
public class OrderEventListener {

    private final CacheService cacheService;

    @KafkaListener(topics = "order.events")
    public void handleOrderEvent(OrderEvent event) {
        if (event instanceof StatusChangedEvent) {
            // Invalidate order cache
            cacheService.delete("orders:" + event.getOrderId());
            log.info("Invalidated order cache: orderId={}", event.getOrderId());
        }
    }
}
```

---

## Messaging Patterns

### Kafka Producer Configuration

```java
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3);  // Retry on failure

        return new DefaultProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Kafka Producer Service

```java
@Service
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_TOPIC = "order.events";
    private static final String ANALYTICS_TOPIC = "analytics.events";

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send analytics event asynchronously (fire-and-forget).
     * Returns immediately without waiting for broker acknowledgment.
     */
    public void sendAnalyticsEvent(AnalyticsEvent event) {
        kafkaTemplate.send(ANALYTICS_TOPIC, event.getUserId(), event);
        log.debug("Analytics event sent: type={}", event.getEventType());
    }

    /**
     * Send order event with callback for error handling.
     */
    public void sendOrderEvent(OrderEvent event) {
        String key = String.valueOf(event.getOrderId()); // Partition by order ID

        ListenableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(ORDER_TOPIC, key, event);

        future.addCallback(
                result -> log.info("Order event sent: orderId={}", event.getOrderId()),
                ex -> log.error("Order event failed: {}", event.getOrderId(), ex)
        );
    }
}
```

### Kafka Consumer

```java
@Component
@Slf4j
public class AnalyticsEventConsumer {

    private final RedisTemplate<String, Long> redisTemplate;

    private static final String ANALYTICS_TOPIC = "analytics.events";
    private static final long TTL_DAYS = 90;

    public AnalyticsEventConsumer(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = ANALYTICS_TOPIC,
            groupId = "analytics-aggregation",
            concurrency = "3"  // 3 threads for parallelism
    )
    public void handleAnalyticsEvent(AnalyticsEvent event) {
        try {
            String key = buildRedisKey(event.getEventType(), LocalDate.now());

            // Increment counter
            redisTemplate.opsForValue().increment(key);

            // Set TTL (90 days)
            redisTemplate.expire(key, Duration.ofDays(TTL_DAYS));

            log.debug("Analytics event aggregated: type={}, key={}",
                    event.getEventType(), key);

        } catch (Exception e) {
            log.error("Failed to process analytics event", e);
            // Consumer won't acknowledge message, allowing retry
        }
    }

    private String buildRedisKey(String eventType, LocalDate date) {
        return "analytics:events:" + eventType + ":" + date;
    }
}
```

### RabbitMQ Producer & Consumer

```java
// Producer
@Component
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;
    private static final String ORDER_QUEUE = "order.processing";

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderProcessingMessage(Long orderId) {
        rabbitTemplate.convertAndSend(ORDER_QUEUE, orderId);
    }
}

// Consumer
@Component
@Slf4j
public class RabbitMQConsumer {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = "order.processing")
    public void handleOrderProcessing(Long orderId) {
        log.info("Processing order: orderId={}", orderId);

        Order order = orderRepository.findById(orderId).orElseThrow();
        // Simulate work: validate inventory, process payment, etc.
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
    }
}
```

---

## Security Patterns

### API Key Authentication

```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ApiKeyCacheService apiKeyCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                // Check cache first
                User user = apiKeyCacheService.getUserByApiKey(apiKey);

                // Set authentication
                ApiKeyAuthentication auth = new ApiKeyAuthentication(user, apiKey);
                auth.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (InvalidApiKeyException e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
```

### Rate Limiting

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String apiKey = (String) auth.getCredentials();

            if (!rateLimitService.allowRequest(apiKey)) {
                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.setHeader("Retry-After", "60");
                response.getWriter().write("Rate limit exceeded");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
```

---

## Error Handling

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException e) {
        log.warn("Order not found", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ORDER_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

### Custom Exceptions

```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}

public class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException(String message) {
        super(message);
    }
}

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
```

---

## Testing Standards

### Service Layer Tests

```java
@SpringBootTest
@Slf4j
class OrderServiceTest {

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private KafkaProducer kafkaProducer;

    @Autowired
    private OrderService orderService;

    @Test
    void testCreateOrder() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                1L,      // userId
                100.00,  // totalAmount
                "123 Main St"
        );

        Order expected = new Order()
                .setOrderNumber("ORD-ABC123")
                .setStatus(OrderStatus.PENDING)
                .setTotalAmount(request.getTotalAmount());

        when(orderRepository.save(any())).thenReturn(expected);

        // Act
        Order actual = orderService.createOrder(request);

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.getOrderNumber()).isEqualTo("ORD-ABC123");

        verify(orderRepository).save(any());
        verify(kafkaProducer).sendOrderEvent(any());
    }
}
```

### Controller Integration Tests

```java
@WebMvcTest(OrderController.class)
@Slf4j
class OrderControllerTest {

    @MockBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetOrderSuccess() throws Exception {
        // Arrange
        Order order = new Order()
                .setId(1L)
                .setOrderNumber("ORD-123");

        when(orderService.getOrderById(1L)).thenReturn(order);

        // Act & Assert
        mockMvc.perform(get("/api/orders/1")
                .header("X-API-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-123"));
    }
}
```

---

## Documentation Standards

### JavaDoc Comments

```java
/**
 * Get order by ID with Redis caching.
 *
 * Implements cache-aside pattern:
 * 1. Check Redis cache
 * 2. If miss: Query PostgreSQL
 * 3. Store result in cache with 30-minute TTL
 *
 * @param id Order ID (must be positive)
 * @return Order entity if found
 * @throws OrderNotFoundException if order doesn't exist
 *
 * @see OrderService#createOrder(CreateOrderRequest)
 */
public Order getOrderById(Long id) {
    // implementation
}
```

### README for Modules

Each domain should have a brief README:

```markdown
# Order Module

Manages order lifecycle from creation to delivery.

## Endpoints

- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Retrieve order
- `PATCH /api/orders/{id}/status` - Update order status

## Database Schema

- **orders** table: Order records
- **order_items** table: Line items

## Events Published

- `OrderCreatedEvent` - Sent to Kafka when order is created
- `StatusChangedEvent` - Sent when order status changes

## Cache Keys

- `orders:{orderId}` - Full order with 30-min TTL
- `orders:user:{userId}:*` - User's orders (paginated)
```

---

## Code Review Checklist

Before submitting a PR, ensure:

- [ ] Code follows naming conventions
- [ ] Classes are in correct layer (controller, service, repo, etc.)
- [ ] Business logic is in service layer, not controller
- [ ] All public methods have JavaDoc
- [ ] Exception handling is appropriate
- [ ] No hardcoded values (use constants or config)
- [ ] DTOs are used for API boundaries
- [ ] Tests cover happy path and edge cases
- [ ] Cache invalidation is correct
- [ ] No N+1 query problems
- [ ] Async operations use Kafka/RabbitMQ, not @Async
- [ ] Rate limiting is enforced for user-facing endpoints
- [ ] Sensitive data (passwords, keys) not logged

---

## Performance Guidelines

| Metric | Target | Implementation |
|--------|--------|----------------|
| API Response (p95) | <200ms | Caching, indexing, async |
| Cache Hit Rate | >90% | Cache-aside pattern |
| Database Queries | <50ms | Indexes, connection pooling |
| Kafka Latency | <100ms | Consumer parallelism |
| Rate Limit Accuracy | >99% | Redis atomic ops |

---

**Standards Version:** 2.0 | **Last Updated:** January 16, 2026 | **Phase:** Phase 8
