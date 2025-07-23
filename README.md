# Transactional Outbox Pattern Implementation

A robust implementation of the Transactional Outbox Pattern for reliable message publishing with support for multiple message brokers.

## Features

- **Multiple Publisher Support**: Choose between Logging, Kafka, and RabbitMQ publishers
- **Ordered and Unordered Messages**: Support for both ordered and unordered event publishing
- **Retry Mechanism**: Configurable retry logic with exponential backoff
- **Health Monitoring**: Built-in health checks and metrics
- **Transactional Safety**: Ensures messages are published reliably with database transactions
- **Version Management**: Support for event versioning and ordered processing

## Message Publishers

### Configuration

Set the publisher type in `application.properties`:

```properties
# Available types: logging, kafka, rabbitmq
outbox.publisher.type=logging
```

Or via environment variable:
```bash
export OUTBOX_PUBLISHER_TYPE=kafka
```

### 1. Logging Publisher (Default)
For development and testing. Messages are logged only.

```properties
outbox.publisher.type=logging
```

### 2. Kafka Publisher
Publishes messages to Apache Kafka topics.

```properties
outbox.publisher.type=kafka
spring.kafka.bootstrap-servers=localhost:9092
```

### 3. RabbitMQ Publisher  
Publishes messages to RabbitMQ queues.

```properties
outbox.publisher.type=rabbitmq
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin
```

## Getting Started

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Gradle 8.x

### Quick Start

1. **Clone the repository**
```bash
git clone <repository-url>
cd outbox-pattern
```

2. **Start infrastructure services**
```bash
docker-compose up -d mysql kafka rabbitmq
```

3. **Run the application with different publishers**

**Using Logging Publisher (default):**
```bash
./gradlew bootRun
```

**Using Kafka Publisher:**
```bash
OUTBOX_PUBLISHER_TYPE=kafka ./gradlew bootRun
```

**Using RabbitMQ Publisher:**
```bash
OUTBOX_PUBLISHER_TYPE=rabbitmq ./gradlew bootRun
```

## API Endpoints

### Demo Controller

**Create Unordered Message:**
```bash
curl -X POST "http://localhost:8080/api/demo/unordered-message?topic=test-topic&message=Hello World"
```

**Create Ordered Message:**
```bash
curl -X POST "http://localhost:8080/api/demo/ordered-message?topic=test-topic&aggregateId=user-123&message=User Created"
```

**Get Metrics:**
```bash
curl -X GET "http://localhost:8080/api/demo/metrics"
```

**Get Publisher Info:**
```bash
curl -X GET "http://localhost:8080/api/demo/publisher-info"
```

**Process Pending Messages:**
```bash
curl -X POST "http://localhost:8080/api/demo/process-pending"
```

## Testing Different Publishers

### 1. Test with Kafka

```bash
# Start services
docker-compose up -d

# Run app with Kafka publisher
OUTBOX_PUBLISHER_TYPE=kafka ./gradlew bootRun

# Create a message
curl -X POST "http://localhost:8080/api/demo/unordered-message?topic=test-events&message=Testing Kafka"

# Process messages
curl -X POST "http://localhost:8080/api/demo/process-pending"

# Check Kafka UI at http://localhost:8081
```

### 2. Test with RabbitMQ

```bash
# Start services  
docker-compose up -d

# Run app with RabbitMQ publisher
OUTBOX_PUBLISHER_TYPE=rabbitmq ./gradlew bootRun

# Create a message
curl -X POST "http://localhost:8080/api/demo/unordered-message?topic=test-queue&message=Testing RabbitMQ"

# Process messages
curl -X POST "http://localhost:8080/api/demo/process-pending"

# Check RabbitMQ Management UI at http://localhost:15672 (admin/admin)
```

### 3. Test with Logging

```bash
# Run app with logging publisher (default)
./gradlew bootRun

# Create a message  
curl -X POST "http://localhost:8080/api/demo/unordered-message?message=Testing Logging"

# Process messages and check logs
curl -X POST "http://localhost:8080/api/demo/process-pending"
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Test Different Publishers

The tests include unit tests for each publisher and integration tests:

- `RabbitMQEventPublisherTest` - Tests RabbitMQ publisher functionality
- `KafkaEventPublisherTest` - Tests Kafka publisher functionality  
- `OutboxIntegrationTest` - Integration tests for the outbox pattern

## Docker Services

The `docker-compose.yml` includes:

- **MySQL**: Database for outbox table
- **Kafka + Zookeeper**: Message streaming platform
- **Kafka UI**: Web interface for Kafka (port 8081)
- **RabbitMQ**: Message broker with management interface (port 15672)

### Service URLs:
- Application: http://localhost:8080
- Kafka UI: http://localhost:8081  
- RabbitMQ Management: http://localhost:15672 (admin/admin)

## Configuration

Key configuration options in `application.properties`:

```properties
# Publisher selection
outbox.publisher.type=logging|kafka|rabbitmq

# Processing configuration
outbox.processing.batch-size=500
outbox.processing.publish-rate=PT10S

# Retry configuration
outbox.retry.max-retries=5
outbox.retry.initial-delay=PT1M
```

## Architecture

The implementation follows the Transactional Outbox Pattern:

1. **Write to Database**: Messages are written to the outbox table in the same transaction as business data
2. **Background Processing**: A scheduled process reads pending messages from the outbox
3. **Message Publishing**: Messages are published to the configured message broker
4. **Status Tracking**: Message status is updated upon successful publishing

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and add tests
4. Run tests: `./gradlew test`
5. Submit a pull request

## License

This project is licensed under the MIT License.