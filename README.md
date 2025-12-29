# Scalable Spring Boot API

Production-ready REST API with PostgreSQL, Redis, RabbitMQ, Kafka, and distributed rate limiting.

## Tech Stack

- **Runtime:** Java 21 (LTS)
- **Framework:** Spring Boot 3.2.1
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **Message Queues:** RabbitMQ 3.13, Apache Kafka 3.x
- **Build Tool:** Maven 3.9+

## Architecture

### Layered Architecture (DDD-Inspired)

```
src/main/java/com/project/
├── api/              # Presentation Layer (Controllers, DTOs, Mappers)
├── domain/           # Domain Layer (Entities, Repositories, Services)
├── infrastructure/   # Infrastructure Layer (JPA, Cache, Messaging)
├── security/         # Security Components (Filters, Rate Limiting)
└── config/           # Application Configuration
```

### Key Features

- ✅ **Stateless Design** - Horizontal scaling ready
- ✅ **Redis Caching** - 90%+ cache hit rate target
- ✅ **API Key Authentication** - SHA-256 hashing with Redis cache
- ✅ **Distributed Rate Limiting** - Multi-tier (BASIC, STANDARD, PREMIUM)
- ✅ **Message Queues** - RabbitMQ for tasks, Kafka for events
- ✅ **Connection Pooling** - HikariCP optimized
- ✅ **Database Migrations** - Flyway versioned migrations
- ✅ **API Documentation** - Interactive Swagger UI with OpenAPI 3.0
- ✅ **Kubernetes Ready** - Health probes, graceful shutdown, HPA

## Prerequisites

- **Java 21 JDK** installed
- **Maven 3.9+** installed
- **Docker Desktop** installed and running
- **PostgreSQL client** (psql) - optional but recommended
- **Redis CLI** (redis-cli) - optional but recommended

## Quick Start

### 1. Start Dependencies

Start all backend services (PostgreSQL, Redis, RabbitMQ, Kafka):

```bash
docker-compose up -d
```

Verify services are running:

```bash
docker-compose ps
```

Expected output: All services should show `healthy` status.

### 2. Run Application

**Option A: Using Maven** (Development)

```bash
mvn spring-boot:run
```

**Option B: Build and Run** (Production-like)

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/scalable-api-1.0.0.jar
```

### 3. Verify Application

**Health Check:**

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

**Actuator Info:**

```bash
curl http://localhost:8080/actuator/info
```

## Available Services

After `docker-compose up`, the following services are available:

| Service   | URL                          | Credentials        |
|-----------|------------------------------|--------------------|
| API       | http://localhost:8080        | -                  |
| Swagger UI| http://localhost:8080/swagger-ui.html | -         |
| OpenAPI Docs | http://localhost:8080/v3/api-docs | -           |
| PostgreSQL| localhost:5432               | postgres/dev_password |
| Redis     | localhost:6379               | No auth            |
| RabbitMQ UI | http://localhost:15672      | guest/guest        |
| Kafka     | localhost:9092               | No auth            |

## Environment Profiles

The application supports multiple profiles:

- **dev** (default): Local development with verbose logging
- **prod**: Production configuration with JSON logging

Switch profiles:

```bash
# Development
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Configuration

Configuration is managed via `application.yml` and profile-specific files:

- `application.yml` - Common configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production configuration (uses env vars)

### Environment Variables (Production)

Required for production deployment:

```bash
# Database
DB_HOST=postgres-host
DB_PORT=5432
DB_NAME=apidb
DB_USER=postgres
DB_PASSWORD=your-secure-password

# Redis
REDIS_HOST=redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# RabbitMQ
RABBITMQ_HOST=rabbitmq-host
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# Kafka
KAFKA_BROKERS=kafka:9092
```

## Database Migrations

Flyway manages database schema migrations.

### Migration Files

Located in `src/main/resources/db/migration/`:

- `V1__init_schema.sql` - Initial tables
- `V2__add_indexes.sql` - Performance indexes
- `V3__seed_data.sql` - Development test data

### Run Migrations

Migrations run automatically on application startup when Flyway is enabled.

### Manual Migration Commands

```bash
# Check migration status
mvn flyway:info

# Run migrations manually
mvn flyway:migrate

# Clean database (WARNING: Deletes all data)
mvn flyway:clean
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Integration Tests Only

```bash
mvn verify -P integration-tests
```

### Skip Tests

```bash
mvn clean package -DskipTests
```

## Build & Deploy

### Build Docker Image

```bash
docker build -t scalable-api:1.0.0 .
```

### Run with Docker

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=host.docker.internal \
  scalable-api:1.0.0
```

### Kubernetes Deployment

Kubernetes manifests are located in `k8s/` directory:

```bash
# Apply all manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods
kubectl get svc
```

## Monitoring

### Actuator Endpoints

- `/actuator/health` - Health check (liveness/readiness probes)
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus-formatted metrics

### Prometheus + Grafana

Start monitoring stack:

```bash
docker-compose -f docker-compose-monitoring.yml up -d
```

Access:
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)

## Troubleshooting

### Application won't start

**Check Docker services are running:**

```bash
docker-compose ps
```

All services should be `healthy`.

**Check logs:**

```bash
# Application logs
mvn spring-boot:run

# Docker service logs
docker-compose logs postgres
docker-compose logs redis
```

### Database connection errors

**Verify PostgreSQL is accessible:**

```bash
psql -h localhost -U postgres -d apidb_dev
```

Password: `dev_password`

### Redis connection errors

**Test Redis connectivity:**

```bash
redis-cli ping
```

Expected: `PONG`

### Clean restart

```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: Deletes all data)
docker-compose down -v

# Start fresh
docker-compose up -d
```

## Project Structure

```
scalable-api/
├── src/
│   ├── main/
│   │   ├── java/com/project/
│   │   │   ├── api/               # REST Controllers, DTOs
│   │   │   ├── domain/            # Business Logic
│   │   │   ├── infrastructure/    # JPA, Cache, Messaging
│   │   │   ├── security/          # Authentication, Rate Limiting
│   │   │   └── config/            # Spring Configuration
│   │   └── resources/
│   │       ├── db/migration/      # Flyway SQL migrations
│   │       ├── application.yml    # Configuration
│   │       └── logback-spring.xml # Logging config
│   └── test/                      # Unit & Integration Tests
├── k8s/                           # Kubernetes manifests
├── docker-compose.yml             # Local development services
├── Dockerfile                     # Multi-stage Docker build
├── pom.xml                        # Maven dependencies
└── README.md                      # This file
```

## Development Workflow

1. **Start services:** `docker-compose up -d`
2. **Run application:** `mvn spring-boot:run`
3. **Make changes** to code
4. **Run tests:** `mvn test`
5. **Build:** `mvn clean package`
6. **Deploy:** Use Docker or Kubernetes

## Performance Targets

- **API Response Time (p95):** <200ms
- **Throughput:** >1,000 requests/sec
- **Cache Hit Rate:** >90%
- **Rate Limit Accuracy:** >99%

## License

MIT

## Support

For issues and questions, refer to the implementation plan documentation in `/plans`.
