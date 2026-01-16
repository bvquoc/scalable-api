# System Architecture - Scalable Spring Boot API

**Last Updated:** January 16, 2026
**Phase:** Phase 8: Demo Use Cases Completion
**Architecture Pattern:** Microservices-Ready, Event-Driven, Cache-Aside

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                                 │
│  (Web Browser, Mobile App, External Services via REST API)          │
└────────────────────┬────────────────────────────────────────────────┘
                     │ HTTP/HTTPS
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    SECURITY LAYER                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  API Key Authentication Filter (X-API-Key header)          │   │
│  │  • SHA-256 validation                                       │   │
│  │  • Redis-backed caching                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Rate Limiting Filter (Bucket4j + Redis)                   │   │
│  │  • BASIC: 60 req/min                                        │   │
│  │  • STANDARD: 300 req/min                                    │   │
│  │  • PREMIUM: 1,000 req/min                                   │   │
│  │  • UNLIMITED: No limit                                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER (Controllers)                    │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ AnalyticsEventController (NEW - Phase 8)                     │ │
│  │ • POST /api/events - Log analytics event (202 Accepted)     │ │
│  │ • GET /api/analytics/summary - Get event summary            │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ OrderController                                              │ │
│  │ • POST /api/orders - Create order                           │ │
│  │ • GET /api/orders/{id} - Get order (cached)                │ │
│  │ • PATCH /api/orders/{id}/status - Update status            │ │
│  │ • GET /api/orders/user/{userId} - User's orders            │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ ProductController                                            │ │
│  │ • GET /api/products - List products (cached)                │ │
│  │ • POST /api/products - Create product                       │ │
│  │ • GET /api/products/search - Search products               │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ UserController                                               │ │
│  │ • GET /api/users - List users (paginated, cached)           │ │
│  │ • POST /api/users - Create user                             │ │
│  │ • GET /api/users/search - Search users                      │ │
│  └───────────────────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│              BUSINESS LOGIC LAYER (Services & Domain)              │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ AnalyticsEventService (NEW)                                  │ │
│  │ • logEvent() → Publish to Kafka                             │ │
│  │ • getSummary() → Aggregate from Redis                       │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ OrderService                                                 │ │
│  │ • Create order → DB + Kafka + RabbitMQ + Cache             │ │
│  │ • Update status → DB + Kafka + Cache invalidation          │ │
│  │ • Get order → Cache-aside pattern (Redis or DB)            │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │ ProductService & UserService                                │ │
│  │ • CRUD operations with Redis caching                        │ │
│  │ • Pagination support                                        │ │
│  │ • Search functionality                                      │ │
│  └───────────────────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────────────────┘
                     │
            ┌────────┼────────┬────────┐
            │        │        │        │
            ▼        ▼        ▼        ▼
    ┌─────────┐ ┌──────┐ ┌──────┐ ┌─────────┐
    │ Redis   │ │ DB   │ │Kafka │ │RabbitMQ│
    │ Cache   │ │(JPA) │ │(Async)│ │(Tasks) │
    └─────────┘ └──────┘ └──────┘ └─────────┘
```

---

## Detailed Component Architecture

### 1. Analytics Domain (NEW - Phase 8)

```
┌──────────────────────────────────────────────────────────────┐
│                    ANALYTICS FLOW                             │
└──────────────────────────────────────────────────────────────┘

CLIENT REQUEST
    │
    │ POST /api/events
    │ {userId, eventType, properties}
    │
    ▼
┌──────────────────────────────────┐
│ AnalyticsEventController         │
│ • Validate input                 │
│ • Return 202 Accepted (fire)     │
└──────────────────────────────────┘
    │
    │ Fire-and-Forget (Async)
    │
    ▼
┌──────────────────────────────────┐
│ AnalyticsEventService            │
│ • Convert DTO → Event            │
│ • Send to Kafka                  │
└──────────────────────────────────┘
    │
    │ KafkaTemplate.send()
    │ Topic: analytics.events
    │
    ▼
┌──────────────────────────────────┐
│ Kafka Topic: analytics.events    │
│ • Partitions: 3                  │
│ • Retention: 7 days              │
│ • Event: {userId, type, props}   │
└──────────────────────────────────┘
    │
    │ Consumed by AnalyticsEventConsumer
    │
    ▼
┌──────────────────────────────────┐
│ AnalyticsEventConsumer           │
│ • Receive event message          │
│ • Increment Redis counter        │
│ • Set TTL (90 days)              │
└──────────────────────────────────┘
    │
    │ Redis key: analytics:events:{type}:{date}
    │
    ▼
┌──────────────────────────────────┐
│ Redis Hash: Daily Aggregation    │
│ • PAGE_VIEW: 1542                │
│ • BUTTON_CLICK: 892              │
│ • FORM_SUBMIT: 245               │
│ • API_CALL: 3891                 │
│ • PURCHASE: 143                  │
└──────────────────────────────────┘

QUERY ANALYTICS
    │
    │ GET /api/analytics/summary?date=2026-01-16
    │
    ▼
┌──────────────────────────────────┐
│ AnalyticsEventController         │
│ • Extract date parameter         │
│ • Call service                   │
└──────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────┐
│ AnalyticsEventService            │
│ • Query Redis for all event types│
│ • Build summary response         │
└──────────────────────────────────┘
    │
    │ Redis GET analytics:events:*:{date}
    │
    ▼
┌──────────────────────────────────┐
│ Response                         │
│ {                                │
│   "date": "2026-01-16",         │
│   "eventCounts": {              │
│     "PAGE_VIEW": 1542,          │
│     "BUTTON_CLICK": 892,        │
│     "FORM_SUBMIT": 245,         │
│     "API_CALL": 3891,           │
│     "PURCHASE": 143             │
│   },                            │
│   "totalEvents": 6713           │
│ }                                │
└──────────────────────────────────┘

PERFORMANCE CHARACTERISTICS:
─────────────────────────────
• Request Latency: <10ms (202 response)
• Processing Latency: <100ms (Kafka → Consumer → Redis)
• Throughput: 10,000+ events/second
• Storage: Redis with 90-day TTL
• Query Performance: <5ms (Redis gets)
```

### 2. Order Processing Flow

```
┌──────────────────────────────────────────────────────────────┐
│              ORDER PROCESSING ARCHITECTURE                   │
└──────────────────────────────────────────────────────────────┘

1. CREATE ORDER
─────────────

POST /api/orders
    │
    ▼
OrderController.createOrder()
    │
    ▼
OrderService.createOrder()
    │
    ├─► 1. Save to PostgreSQL (via JPA)
    │   └─► Order entity + OrderItems
    │
    ├─► 2. Publish OrderCreatedEvent to Kafka
    │   └─► Topic: order.events
    │       Partition by userId
    │
    └─► 3. Send OrderProcessingMessage to RabbitMQ
        └─► Queue: order.processing
            Worker polls and processes


2. ORDER LIFECYCLE
──────────────────

Order Created (PENDING)
    │
    ├─► Cache in Redis: orders:{orderId}
    │
    ├─► RabbitMQ Worker
    │   • Validates inventory
    │   • Processes payment
    │   • Updates stock
    │
    └─► Order Status Changes (PROCESSING → SHIPPED → DELIVERED)
        │
        ├─► PostgreSQL update
        ├─► Kafka event: StatusChangedEvent
        └─► Cache invalidation


3. READ OPERATION (Cache-Aside)
────────────────────────────────

GET /api/orders/{id}
    │
    ▼
OrderService.getOrder(id)
    │
    ├─► Check Redis cache
    │   Key: orders:{orderId}
    │
    ├─IF CACHE HIT:
    │   └─► Return cached order (5ms)
    │
    └─IF CACHE MISS:
        │
        ├─► Query PostgreSQL (50ms)
        │
        ├─► Store in Redis (TTL: 30 minutes)
        │
        └─► Return order


4. STATUS UPDATE
────────────────

PATCH /api/orders/{id}/status
    │
    ▼
OrderService.updateStatus()
    │
    ├─► Update PostgreSQL
    │
    ├─► Publish StatusChangedEvent to Kafka
    │
    ├─► Invalidate cache: DEL orders:{orderId}
    │   (Next read will refresh from DB)
    │
    └─► Return updated order
```

### 3. Cache-Aside Pattern (Read-Heavy Operations)

```
┌──────────────────────────────────────────────────────────────┐
│               CACHE-ASIDE PATTERN                             │
└──────────────────────────────────────────────────────────────┘

READ PATH:
──────────

Request for Data
    │
    ▼
Check Redis Cache
    │
    ├─[CACHE HIT]──► Return cached data (5ms)
    │
    └─[CACHE MISS]──► Query PostgreSQL
                      │
                      ├─► Store in Redis (TTL: 1h-30m)
                      │
                      └─► Return fresh data (50ms)

WRITE PATH:
───────────

Update PostgreSQL
    │
    ├─► Publish Event to Kafka (async)
    │
    ├─► Invalidate Cache: DEL cache_key
    │   (Lazy refresh on next read)
    │
    └─► Return updated data

CACHE KEYS BY DOMAIN:
─────────────────────

Users:
  • users:{userId}
  • user-list:{page}-{size}

Products:
  • products:{productId}
  • product-list:{page}-{size}

Orders:
  • orders:{orderId}
  • orders:user:{userId}:{page}-{size}

API Keys:
  • apikey:{rawKey}:{hash}

Analytics (Aggregated):
  • analytics:events:{type}:{date}
```

### 4. Message Queue Architecture

```
┌──────────────────────────────────────────────────────────────┐
│           MESSAGE QUEUE TOPOLOGY                              │
└──────────────────────────────────────────────────────────────┘

KAFKA (Event Streaming)
──────────────────────

Topic: analytics.events
  │
  ├─ Partitions: 3 (fan-out to multiple consumers)
  ├─ Retention: 7 days
  ├─ Producer: AnalyticsEventService
  │
  └─ Consumers:
     ├─ AnalyticsEventConsumer
     │  └─► Aggregate to Redis
     │      (analytics:events:{type}:{date})
     │
     └─ Optional: Audit/Storage consumers


Topic: order.events
  │
  ├─ Partitions: 3
  ├─ Retention: 7 days
  ├─ Producer: OrderService
  │  └─► Events: OrderCreatedEvent, StatusChangedEvent
  │
  └─ Consumers:
     ├─ OrderEventConsumer
     │  └─► Saga pattern, workflow triggers
     │
     ├─ AnalyticsConsumer
     │  └─► Aggregate for reporting
     │
     └─ AuditConsumer
        └─► Immutable audit log


RABBITMQ (Task Queue / Work Distribution)
──────────────────────────────────────────

Queue: order.processing
  │
  ├─ Exchange: amq.direct
  ├─ Routing key: order.process
  ├─ Durability: Yes (survives restart)
  ├─ Producer: OrderService
  │  └─► OrderProcessingMessage {orderId, items, amount}
  │
  └─ Consumer Worker Pool:
     ├─► Validate inventory
     ├─► Process payment
     ├─► Update stock
     ├─► Publish completion event


Queue: notifications
  │
  ├─ Exchange: amq.direct
  ├─ Routing key: notify
  ├─ Producer: Services (Order, User)
  │  └─► EmailMessage, SMSMessage
  │
  └─ Consumer:
     └─► Send email/SMS asynchronously
         (decouples from API response)

MESSAGE FLOW DIAGRAM:
────────────────────

API Request
    │
    ├─► Process immediately → HTTP Response
    │
    ├─► Publish to Kafka (fire-and-forget)
    │   └─► Multiple consumers process in parallel
    │
    └─► Send to RabbitMQ (guaranteed delivery)
        └─► Worker pool processes reliably
            (can retry on failure)
```

### 5. Authentication & Authorization

```
┌──────────────────────────────────────────────────────────────┐
│         SECURITY ARCHITECTURE                                │
└──────────────────────────────────────────────────────────────┘

API KEY AUTHENTICATION:
──────────────────────

REQUEST: GET /api/users
Headers: X-API-Key: test-api-key-local-dev

    │
    ▼
ApiKeyAuthenticationFilter
    │
    ├─► Extract header: X-API-Key
    │
    ├─► Compute SHA-256 hash
    │
    ├─► Check Redis cache
    │   Key: apikey:{rawKey}:{hash}
    │
    ├─[CACHE HIT]──► Load User & Tier
    │                 └─► TTL: 30 minutes
    │
    └─[CACHE MISS]──► Query PostgreSQL: api_keys table
                      │
                      ├─► Validate hash
                      ├─► Load associated User
                      ├─► Determine Tier (BASIC, STANDARD, PREMIUM, UNLIMITED)
                      │
                      └─► Cache result for 30 minutes

    │
    ▼
ApiKeyAuthentication token
    │
    ├─ Principal: User
    ├─ Credentials: API key hash
    └─ Authorities: {TIER_BASIC, TIER_STANDARD, ...}

    │
    ▼
RateLimitFilter
    │
    ├─► Look up rate limit tier
    │
    ├─► Redis: Rate limit counter
    │   Key: ratelimit:{apiKey}:{minute}
    │
    ├─► Check bucket: Current tokens >= cost
    │
    ├─[ALLOWED]──► Decrement token, proceed
    │
    └─[REJECTED]──► Return 429 Too Many Requests
                    Header: Retry-After: 45


TIER LIMITS:
────────────

BASIC:      60 requests/minute
STANDARD:   300 requests/minute
PREMIUM:    1,000 requests/minute
UNLIMITED:  No limit


PUBLIC ENDPOINTS (No auth):
───────────────────────────

✓ /actuator/health
✓ /actuator/info
✓ /swagger-ui.html
✓ /v3/api-docs

Protected Endpoints:
▓ /api/**
▓ /actuator/metrics
▓ /actuator/prometheus
```

### 6. Database Layer Architecture

```
┌──────────────────────────────────────────────────────────────┐
│           DATA PERSISTENCE LAYER                              │
└──────────────────────────────────────────────────────────────┘

PostgreSQL 16
    │
    ├─ Connection Pool: HikariCP
    │  ├─ Max Pool Size: 20
    │  ├─ Min Idle: 5
    │  └─ Connection Timeout: 30s
    │
    ├─ Database: apidb (dev), apidb_prod (prod)
    │
    └─ Tables:
       │
       ├─ users
       │  ├─ PK: id (UUID)
       │  ├─ email (UNIQUE)
       │  ├─ username (UNIQUE)
       │  ├─ full_name
       │  ├─ status (ACTIVE/INACTIVE)
       │  └─ Indexes: email, username
       │
       ├─ api_keys
       │  ├─ PK: id
       │  ├─ FK: user_id
       │  ├─ api_key_hash (UNIQUE)
       │  ├─ tier (BASIC/STANDARD/PREMIUM/UNLIMITED)
       │  └─ Indexes: user_id, api_key_hash
       │
       ├─ products
       │  ├─ PK: id
       │  ├─ name
       │  ├─ sku (UNIQUE)
       │  ├─ price
       │  ├─ stock_quantity
       │  ├─ category
       │  └─ Indexes: sku, category
       │
       ├─ orders
       │  ├─ PK: id
       │  ├─ FK: user_id
       │  ├─ order_number (UNIQUE)
       │  ├─ status (PENDING/PROCESSING/SHIPPED/DELIVERED)
       │  ├─ total_amount
       │  ├─ shipping_address
       │  └─ Indexes: user_id, order_number, status
       │
       └─ order_items (JOIN TABLE)
          ├─ PK: (order_id, product_id)
          ├─ FK: order_id, product_id
          ├─ quantity
          ├─ unit_price
          └─ Indexes: order_id, product_id

FLYWAY MIGRATIONS:
──────────────────

V1__init_schema.sql
   └─► Create tables, PKs, FKs, basic indexes

V2__add_indexes.sql
   └─► Performance optimization indexes

V3__seed_data.sql
   └─► Test data (users, products, orders)

V4__add_analytics.sql (NEW - Phase 8)
   └─► Analytics event schema preparation


JPA/REPOSITORY PATTERN:
──────────────────────

Entity Classes:
  • User (OneToMany: ApiKey, Order)
  • Order (OneToMany: OrderItem, ManyToOne: User)
  • Product (OneToMany: OrderItem)
  • OrderItem (ManyToOne: Order, Product)
  • ApiKey (ManyToOne: User)

Repository Interfaces:
  • UserRepository extends JpaRepository<User, Long>
  • OrderRepository extends JpaRepository<Order, Long>
  • ProductRepository extends JpaRepository<Product, Long>
  • ApiKeyRepository extends JpaRepository<ApiKey, Long>

Query Methods:
  • findByEmail()
  • findByOrderNumber()
  • findBySku()
  • findByUserId() + Pageable
```

---

## Data Flow Diagrams

### Complete Request Lifecycle

```
CLIENT REQUEST
    │
    ├─► HTTP GET /api/orders/1
    │   Headers: X-API-Key: test-api-key-local-dev
    │
    ▼
API GATEWAY / LOAD BALANCER
    │
    ▼
SPRING SERVLET FILTER CHAIN
    │
    ├─► ApiKeyAuthenticationFilter
    │   │
    │   ├─ Check Redis cache for API key
    │   ├─ Validate hash
    │   └─ Set authentication principal
    │
    ├─► RateLimitFilter
    │   │
    │   ├─ Look up tier from auth
    │   ├─ Check Redis rate limit token bucket
    │   └─ Allow or reject (429)
    │
    ▼
DISPATCHER SERVLET
    │
    ▼
ORDER CONTROLLER
    │
    ├─► Validate path parameter: {id=1}
    │
    ▼
ORDER SERVICE (Business Logic)
    │
    ├─► Check Redis cache
    │   Key: orders:1
    │   │
    │   ├─[HIT]──► Return cached JSON
    │   │           Response time: 5ms
    │   │
    │   └─[MISS]──► Continue...
    │
    ├─► Query PostgreSQL
    │   │
    │   ├─ SELECT * FROM orders WHERE id = 1
    │   ├─ Fetch related order_items
    │   ├─ Hydrate Order entity
    │   │
    │   └─ Response time: 50ms
    │
    ├─► Cache in Redis
    │   │
    │   ├─ Key: orders:1
    │   ├─ Value: JSON serialized Order
    │   ├─ TTL: 30 minutes
    │   │
    │   └─ Store response time: 2ms
    │
    ▼
ORDER RESPONSE MAPPER
    │
    ├─► Convert Order entity → OrderResponse DTO
    │
    ▼
HTTP RESPONSE (200 OK)
    │
    ├─ Body: OrderResponse JSON
    ├─ Headers: Content-Type: application/json
    │           Cache-Control: max-age=1800
    │
    └─ Total latency: 5-60ms
       (depending on cache hit/miss)
```

### Asynchronous Event Processing

```
SYNCHRONOUS (Request-Response):
───────────────────────────────

POST /api/events
{userId, eventType, properties}
    │
    ├─► Validate & authenticate (10ms)
    │
    ├─► Service receives request
    │
    ├─► Kafka producer sends (non-blocking)
    │   (adds to local buffer, returns immediately)
    │
    └─► Return 202 Accepted (0-5ms)

    Total: <20ms


ASYNCHRONOUS (Background Processing):
──────────────────────────────────────

Kafka Broker receives message
    │
    ├─► Replicates to partitions
    │
    ▼
Kafka Consumer (AnalyticsEventConsumer)
    │
    ├─► Poll message (batched)
    │
    ├─► Deserialize AnalyticsEvent
    │
    ├─► Extract: eventType, date
    │
    ├─► Build Redis key: analytics:events:{type}:{date}
    │
    ├─► INCR in Redis (increment counter)
    │
    ├─► EXPIRE key at 90 days
    │
    └─► Commit offset (Kafka tracking)

    Total: <100ms
    (Can be parallelized across 3 partitions)
```

---

## Scalability Considerations

### Horizontal Scaling Strategy

```
STATELESS APPLICATION INSTANCES:
────────────────────────────────

Instance 1    Instance 2    Instance 3
    │             │             │
    └─────────────┼─────────────┘
                  │
        Load Balancer (Round Robin / Sticky)
                  │
            ┌─────┴─────┐
            │           │
        Redis         PostgreSQL
       (Shared)       (Shared)
            │           │
            │     Flyway Migrations
            │     (Schema version)
            │
            └─► All instances read same data
                All instances access same cache
                All instances update same DB

Benefits:
  • Add/remove instances without state migration
  • No server affinity needed
  • Scales with load
  • Fault tolerant (instance failure = minimal impact)
```

### Redis Caching Topology

```
CACHE DISTRIBUTION:
───────────────────

All Instances ──┐
                ├─► Single Redis Instance (or Cluster)
                │
                ├─ Shared cache space
                ├─ TTL-based expiration
                └─ No cache coherency issues
                   (eventual consistency acceptable)

FOR HIGH AVAILABILITY:

Instance 1 ──┐
             ├─► Redis Cluster (3+ nodes)
Instance 2 ──┤   • Replication
             ├─► • Automatic failover
Instance 3 ──┘   • 99.9% uptime
```

### Database Connection Pooling

```
HikariCP Configuration:
───────────────────────

Max Pool Size: 20
Min Idle: 5
Connection Timeout: 30s
Idle Timeout: 10 minutes

Instances:
Instance 1: 5 connections
Instance 2: 5 connections
Instance 3: 5 connections
────────────────────────
Total: 15 connections (optimized)
Max possible: 60 (if all instances fully loaded)

Database can handle 200+ connections
So no bottleneck at database tier
```

---

## Monitoring & Observability Stack

```
┌──────────────────────────────────────────────────────────────┐
│         OBSERVABILITY ARCHITECTURE                            │
└──────────────────────────────────────────────────────────────┘

APPLICATION LAYER:
──────────────────

Spring Boot Actuator
    │
    ├─ /actuator/health
    │  └─ Liveness (is app alive?)
    │     Readiness (can accept requests?)
    │
    ├─ /actuator/metrics
    │  ├─ http_server_requests_seconds
    │  │  └─ Latency distribution
    │  │
    │  ├─ cache_gets_total, cache_puts_total
    │  │  └─ Cache hit/miss ratio
    │  │
    │  ├─ hikaricp_connections_active
    │  │  └─ DB connection pool usage
    │  │
    │  └─ jvm_memory_used
    │     └─ Heap/non-heap memory
    │
    └─ /actuator/prometheus
       └─ Prometheus text format


METRICS COLLECTION:
───────────────────

Prometheus Server
    │
    ├─ Scrapes /actuator/prometheus every 15s
    │
    ├─ Stores time-series data
    │
    └─ Default retention: 15 days


VISUALIZATION:
──────────────

Grafana Dashboard
    │
    ├─ Data source: Prometheus
    │
    ├─ Pre-built dashboards:
    │  ├─ Spring Boot Metrics
    │  │  ├─ Request rate (req/s)
    │  │  ├─ p95 latency
    │  │  ├─ HTTP status codes
    │  │  ├─ Memory usage
    │  │  └─ Thread count
    │  │
    │  └─ Database metrics
    │     ├─ Active connections
    │     ├─ Query latency
    │     └─ Connection wait time
    │
    └─ Real-time dashboards (auto-refresh)


LOGGING:
────────

Logback (SLF4J implementation)
    │
    ├─ Dev profile: Text format (readable)
    │
    ├─ Prod profile: JSON format (structured)
    │  {
    │    "timestamp": "2026-01-16T22:30:45.123Z",
    │    "level": "INFO",
    │    "logger": "com.project.OrderService",
    │    "message": "Order created",
    │    "orderId": 123,
    │    "userId": 456
    │  }
    │
    └─ Can be parsed by log aggregators
       (ELK, DataDog, Splunk, etc.)


KUBERNETES PROBES:
──────────────────

Liveness Probe:
    GET /actuator/health/liveness
    └─ If unhealthy: kill pod, restart

Readiness Probe:
    GET /actuator/health/readiness
    └─ If not ready: remove from load balancer
```

---

## Security Layers

```
┌──────────────────────────────────────────────────────────────┐
│              DEFENSE IN DEPTH                                │
└──────────────────────────────────────────────────────────────┘

LAYER 1: Transport Security
───────────────────────────
  • HTTPS/TLS in production
  • Certificates from trusted CAs
  • Prevent MITM attacks

LAYER 2: API Key Authentication
───────────────────────────────
  • X-API-Key header validation
  • SHA-256 hashing
  • Redis-backed cache (avoid repeated DB queries)
  • API keys rotated periodically

LAYER 3: Rate Limiting
──────────────────────
  • Token bucket algorithm (Bucket4j)
  • Per-API-key limits
  • Graceful degradation (429 responses)
  • Prevents DDoS / resource exhaustion

LAYER 4: Input Validation
────────────────────────
  • Jakarta validation annotations (@Valid)
  • DTO validation in controllers
  • Exception handling with error responses

LAYER 5: Database Security
──────────────────────────
  • Prepared statements (JPA/Hibernate)
  • Prevents SQL injection
  • Parameterized queries

LAYER 6: Error Handling
──────────────────────
  • GlobalExceptionHandler catches all exceptions
  • No stack traces exposed to clients
  • Error logs contain full details (internal)

LAYER 7: Application Firewall
──────────────────────────────
  • Spring Security filters
  • CORS configuration
  • CSRF tokens (if applicable)
```

---

## Deployment Topology

### Docker Deployment

```
┌─────────────────────────────────────────────────────┐
│              DOCKER CONTAINER STACK                 │
├─────────────────────────────────────────────────────┤
│ scalable-api (Java 21, Spring Boot)                │
│ • Exposes port 8080                                 │
│ • Environment variables for config                  │
│ • Health probes configured                          │
├─────────────────────────────────────────────────────┤
│ postgresql:16                                       │
│ • Port 5432                                         │
│ • Volume: postgres_data                             │
├─────────────────────────────────────────────────────┤
│ redis:7                                             │
│ • Port 6379                                         │
│ • Volume: redis_data                                │
├─────────────────────────────────────────────────────┤
│ rabbitmq:3.13                                       │
│ • Port 5672 (AMQP)                                 │
│ • Port 15672 (Management UI)                        │
├─────────────────────────────────────────────────────┤
│ confluentinc/cp-kafka:7.x                          │
│ • Port 9092                                         │
│ • Broker configuration                              │
├─────────────────────────────────────────────────────┤
│ confluentinc/cp-zookeeper:7.x                      │
│ • Port 2181                                         │
│ • Kafka coordination                                │
├─────────────────────────────────────────────────────┤
│ MONITORING (docker-compose-monitoring.yml)         │
│ • prometheus:latest                                 │
│ • grafana:latest                                    │
└─────────────────────────────────────────────────────┘
```

### Kubernetes Deployment

```
Namespace: default (or custom)

Deployment: scalable-api
  • Replicas: 3 (high availability)
  • Pod template:
    - Container: scalable-api:1.0.0
    - Resources: CPU/Memory limits
    - Health probes: Liveness & readiness
    - Env vars: Config from ConfigMap/Secret

Service: scalable-api-service
  • Type: ClusterIP (internal) or LoadBalancer
  • Port: 8080
  • Selector: app=scalable-api

ConfigMap: scalable-api-config
  • application.yml properties
  • Non-sensitive configuration

Secret: scalable-api-secrets
  • DB_PASSWORD
  • REDIS_PASSWORD
  • RABBITMQ_PASSWORD
  • API_KEY_SIGNING_SECRET

HorizontalPodAutoscaler: scalable-api-hpa
  • Min replicas: 2
  • Max replicas: 10
  • Target CPU: 70%
  • Target Memory: 80%

Persistent Volumes:
  • PostgreSQL data (50GB)
  • Redis data (10GB)
```

---

## Performance Characteristics

### Latency Profile

| Operation | Latency (p95) | Cache Hit | Notes |
|-----------|---------------|-----------|-------|
| Health check | <5ms | N/A | No I/O |
| API key auth | 10-15ms | Cached | Redis lookup |
| Rate limit check | 5-10ms | N/A | Redis atomic ops |
| Log analytics event | 10-20ms | N/A | Returns 202 immediately |
| Get cached order | 5-10ms | Yes | Redis GET |
| Get uncached order | 50-100ms | No | PostgreSQL + Redis SET |
| List products (cached) | 5-15ms | Yes | Redis GET |
| Create order | 100-200ms | No | DB + Kafka + RabbitMQ |
| Database query | 30-50ms | N/A | With indexes |
| Kafka publish | <5ms | N/A | Async, buffered |

### Throughput Estimates

| Metric | Value | Conditions |
|--------|-------|-----------|
| Requests/sec | 1,000+ | Read-heavy, cached |
| Analytics events/sec | 10,000+ | Async, fire-and-forget |
| DB connections | 20/instance | HikariCP pool size |
| Redis ops/sec | 100,000+ | Hash/Set operations |
| Kafka throughput | 100k msg/sec | Depends on broker count |

---

## Glossary

| Term | Definition |
|------|-----------|
| TTL | Time-To-Live (cache expiration) |
| HikariCP | Connection pool library |
| Fire-and-Forget | Send without waiting for response |
| Cache-Aside | Load data from cache, miss → load from DB |
| Bucket4j | Rate limiting library (token bucket) |
| CacheEvict | Invalidate cache entry |
| Kafka topic | Named channel for pub-sub messaging |
| RabbitMQ queue | Named buffer for work distribution |
| Partition | Division of Kafka topic for parallelism |
| Consumer | Process that reads from Kafka topic |

---

**Architecture Version:** 2.0 | **Last Updated:** January 16, 2026 | **Status:** Production-Ready
