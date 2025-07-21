# Enterprise Outbox Pattern Implementation

A production-ready Spring Boot application implementing the **Transactional Outbox Pattern** for reliable message publishing in distributed systems. This implementation includes enterprise features like monitoring, metrics, dead letter queues, and comprehensive error handling.

## 🎯 Overview

The Outbox Pattern ensures reliable message publishing by storing events in a local database table (the "outbox") within the same transaction as business data changes. This enterprise implementation provides:

- **Guaranteed Message Delivery**: Either both business operation and event are persisted, or neither
- **Order Preservation**: Messages can be processed in order per aggregate
- **Retry Logic**: Configurable exponential backoff with dead letter queue support
- **Monitoring & Observability**: Comprehensive metrics and health checks
- **High Performance**: Optimized batch processing and connection pooling
- **Multiple Publishers**: Support for Kafka, RabbitMQ, and custom publishers

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────┐    ┌─────────────────┐
│   Business      │    │   Outbox     │    │   Message       │
│   Service       │───▶│   Table      │───▶│   Broker        │
│                 │    │              │    │   (Kafka/etc)   │
└─────────────────┘    └──────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────┐
                       │  Scheduler   │
                       │  Processor   │
                       └──────────────┘
```

## 🚀 Enterprise Features

### ✅ Core Features
- Transactional event storage with ACID guarantees
- Scheduled processing with configurable intervals
- Ordered and unordered message processing
- JPA/Hibernate integration with optimized queries
- Comprehensive test coverage (>90%)

### ✅ Enterprise Enhancements
- **Dead Letter Queue**: Automatic handling of permanently failed messages
- **Monitoring**: Health checks, metrics, and Prometheus integration
- **Security**: Actuator endpoint protection and audit logging
- **Performance**: Batch processing, connection pooling, and query optimization
- **Configuration**: Environment-specific profiles and feature flags
- **Multiple Publishers**: Kafka, RabbitMQ, and custom implementations

## 📋 Prerequisites

- **Java 21** or higher
- **MySQL 8.0+** or **PostgreSQL 13+**
- **Gradle 8.0+**
- **Docker** (for local development)

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd enterprise-outbox-pattern
```

### 2. Database Setup

#### MySQL (Production)
```sql
CREATE DATABASE outbox_enterprise;
CREATE USER 'outbox_user'@'%' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON outbox_enterprise.* TO 'outbox_user'@'%';
FLUSH PRIVILEGES;
```

#### Using Docker Compose
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: admin
      MYSQL_DATABASE: outbox_enterprise
      MYSQL_USER: outbox_user
      MYSQL_PASSWORD: secure_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

### 3. Environment Configuration

#### Development Profile
```bash
export SPRING_PROFILES_ACTIVE=development
export DB_HOST=localhost
export DB_PORT=3306
export DB_DATABASE=outbox_enterprise
export DB_USERNAME=outbox_user
export DB_PASSWORD=secure_password
```

#### Production Profile
```bash
export SPRING_PROFILES_ACTIVE=production
export DB_HOST=prod-db-host
export DB_PORT=3306
export DB_DATABASE=outbox_enterprise
export DB_USERNAME=outbox_user
export DB_PASSWORD=complex_secure_password
export OUTBOX_BATCH_SIZE=1000
export OUTBOX_PUBLISH_RATE=5
export PROMETHEUS_ENABLED=true
```

### 4. Build & Run

```bash
# Build the project
./gradlew build

# Run with development profile
./gradlew bootRun --args='--spring.profiles.active=development'

# Run with production profile
./gradlew bootRun --args='--spring.profiles.active=production'
```

## 📊 Configuration

### Core Outbox Settings
```properties
# Processing Configuration
outbox.processing.enabled=true
outbox.processing.batch-size=500
outbox.processing.publish-rate=PT10S
outbox.processing.lock-timeout=PT30S

# Retry Configuration
outbox.retry.max-retries=5
outbox.retry.initial-delay=PT1M
outbox.retry.multiplier=2.0
outbox.retry.max-delay=PT24H

# Publisher Configuration
outbox.publisher.type=kafka  # logging, kafka, rabbitmq
outbox.publisher.default-topic=enterprise-events
outbox.publisher.timeout=PT30S

# Monitoring Configuration
outbox.monitoring.enabled=true
outbox.monitoring.metrics-interval=PT1M
```

### Database Configuration
```properties
# Connection Pool (Production)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA Optimizations
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

## 🔧 Usage Examples

### Basic Message Publishing

```java
@Service
public class UserService {
    
    @Autowired
    private OutboxService outboxService;
    
    @Transactional
    public void createUser(CreateUserRequest request) {
        // 1. Perform business operation
        User user = userRepository.save(new User(request));
        
        // 2. Create outbox message in same transaction
        UserCreatedEvent event = new UserCreatedEvent(user.getId(), user.getEmail());
        outboxService.createUnOrderedMessage("user-events", user.getId(), event);
        
        // Both operations succeed or fail together
    }
}
```

### Ordered Message Processing

```java
@Transactional
public void updateUserProfile(String userId, UpdateProfileRequest request) {
    // Business operation
    User user = userRepository.findById(userId);
    user.updateProfile(request);
    userRepository.save(user);
    
    // Ordered event (maintains sequence per user)
    ProfileUpdatedEvent event = new ProfileUpdatedEvent(userId, request);
    outboxService.createOrderedMessage("user-events", userId, event);
}
```

### Message with Headers

```java
@Transactional
public void processPayment(PaymentRequest request) {
    Payment payment = processPaymentLogic(request);
    
    Map<String, String> headers = Map.of(
        "correlation-id", request.getCorrelationId(),
        "source", "payment-service",
        "version", "v1"
    );
    
    PaymentProcessedEvent event = new PaymentProcessedEvent(payment);
    outboxService.createMessage("payment-events", payment.getId(), event, headers);
}
```

## 📈 Monitoring & Operations

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Outbox-specific health
curl http://localhost:8080/actuator/health/outbox
```

### Metrics (Prometheus)
```bash
# Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Key metrics:
# - outbox_messages_created_total
# - outbox_messages_published_total
# - outbox_messages_failed_total
# - outbox_processing_duration_seconds
# - outbox_messages_pending (gauge)
# - outbox_messages_failed (gauge)
```

### Logging
```bash
# Enable debug logging for outbox components
logging.level.com.github.mahdim1000.outboxpattern=DEBUG

# Monitor processing
tail -f logs/application.log | grep "Outbox"
```

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Run with Coverage
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Integration Tests
```bash
# Run only integration tests
./gradlew test --tests "*IntegrationTest"

# Run with test containers (requires Docker)
./gradlew test -Dspring.profiles.active=testcontainers
```

## 🐳 Docker Deployment

### Build Docker Image
```dockerfile
FROM openjdk:21-jre-slim

COPY build/libs/enterprise-outbox-pattern-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose for Production
```yaml
version: '3.8'
services:
  outbox-app:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: production
      DB_HOST: mysql
      DB_USERNAME: outbox_user
      DB_PASSWORD: secure_password
      OUTBOX_PUBLISHER_TYPE: kafka
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - mysql
      - kafka
    ports:
      - "8080:8080"

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: admin
      MYSQL_DATABASE: outbox_enterprise
      MYSQL_USER: outbox_user
      MYSQL_PASSWORD: secure_password
    volumes:
      - mysql_data:/var/lib/mysql

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

volumes:
  mysql_data:
```

## 🔧 Performance Tuning

### Database Optimizations
```sql
-- Indexes for performance
CREATE INDEX idx_outbox_status_next_retry ON outbox(status, next_retry_at);
CREATE INDEX idx_outbox_aggregate_version ON outbox(aggregate_id, version);
CREATE INDEX idx_outbox_created_at ON outbox(created_at);

-- Partition by date for large tables
ALTER TABLE outbox PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026)
);
```

### JVM Tuning
```bash
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Configuration Tuning
```properties
# High-throughput configuration
outbox.processing.batch-size=2000
outbox.processing.publish-rate=PT5S
spring.datasource.hikari.maximum-pool-size=50
spring.jpa.properties.hibernate.jdbc.batch_size=100
```

## 🛡️ Security Considerations

### Database Security
- Use dedicated database user with minimal privileges
- Enable SSL/TLS for database connections
- Regular security updates and patches

### Application Security
- Secure actuator endpoints with authentication
- Use environment variables for sensitive configuration
- Implement audit logging for critical operations

### Network Security
- Run in private networks/VPCs
- Use service mesh for inter-service communication
- Implement proper firewall rules

## 📚 API Documentation

### OutboxService API

```java
public interface OutboxService {
    // Create unordered message
    void createUnOrderedMessage(String topic, String aggregateId, Object payload);
    
    // Create ordered message (maintains sequence)
    void createOrderedMessage(String topic, String aggregateId, Object payload);
    
    // Create message with custom headers
    void createMessage(String topic, String aggregateId, Object payload, Map<String, String> headers);
    
    // Get metrics
    OutboxMetrics getMetrics();
    
    // Version management
    Integer getNextExpectedVersion(String aggregateId);
    boolean canProcessVersion(String aggregateId, Integer version);
}
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Additional Resources

- [Outbox Pattern Documentation](https://microservices.io/patterns/data/transactional-outbox.html)
- [Spring Boot Reference](https://spring.io/projects/spring-boot)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Prometheus Monitoring](https://prometheus.io/docs/)