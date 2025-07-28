# Transactional Outbox Pattern

A robust, production-ready implementation of the Transactional Outbox Pattern in Java and Spring Boot. This project ensures reliable message delivery and at-least-once semantics by atomically committing business data and outgoing messages in a single transaction. It supports multiple message brokers and is designed for scalability and resilience.

## Features

-   **Multiple Message Broker Support**: Seamlessly switch between **Kafka**, **RabbitMQ**, and a **Logging** publisher for development.
-   **Transactional Integrity**: Guarantees that messages are published if and only if the business transaction succeeds.
-   **Concurrent Processing**: Handles high-throughput scenarios with configurable, concurrent message processing.
-   **Ordered & Unordered Messages**: Supports both strictly ordered and unordered message delivery to meet diverse use cases.
-   **Configurable Retry Logic**: Includes exponential backoff and a configurable number of retries for handling transient failures.
-   **Dynamic Topic/Queue Routing**: Publish messages to different topics or queues on a per-message basis.
-   **Extensible Design**: Easily add new message publishers or customize existing ones.
-   **Dockerized Environment**: Includes a `docker-compose` setup for all required infrastructure (MySQL, Kafka, RabbitMQ).

## Architecture

This project implements the **Transactional Outbox Pattern** to solve the dual-write problem, where a service needs to update its own database and publish a message to a message broker as part of a single atomic operation.

1.  **Atomic Write**: When a business operation is performed, a corresponding event is created and stored in an `outbox` table within the same database transaction. This ensures that the event is only saved if the business data is successfully committed.
2.  **Message Relay**: A separate, asynchronous process periodically scans the `outbox` table for unpublished events.
3.  **Guaranteed Delivery**: The relay reads new events and publishes them to the configured message broker (e.g., Kafka or RabbitMQ).
4.  **State Tracking**: Upon successful delivery to the broker, the event is marked as processed in the `outbox` table. If delivery fails, a configurable retry mechanism is activated.

This architecture prevents data inconsistencies and ensures that critical events are never lost.

## Getting Started

### Prerequisites

-   Java 21+
-   Docker and Docker Compose
-   Gradle 8.x or later

### Quick Start

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/mahdim1000/outbox-pattern.git
    cd outbox-pattern
    ```

2.  **Start Infrastructure Services**
    The `docker-compose.yml` file includes all necessary services: MySQL, Kafka, and RabbitMQ.
    ```bash
    docker-compose up -d
    ```

3.  **Run the Application**
    You can run the application with your desired message publisher by setting the `outbox.publisher.type` property or the `OUTBOX_PUBLISHER_TYPE` environment variable.

    -   **Using Kafka (Default)**
        ```bash
        ./gradlew bootRun
        ```
    -   **Using RabbitMQ**
        ```bash
        OUTBOX_PUBLISHER_TYPE=rabbitmq ./gradlew bootRun
        ```
    -   **Using Logging Publisher** (for development/testing)
        ```bash
        OUTBOX_PUBLISHER_TYPE=logging ./gradlew bootRun
        ```

## Configuration

The application's behavior can be customized through `application.properties` or environment variables.

### Publisher Configuration

Set the active publisher and its properties.

| Property                        | Environment Variable                | Description                                         | Default         |
| ------------------------------- | ----------------------------------- | --------------------------------------------------- | --------------- |
| `outbox.publisher.type`         | `OUTBOX_PUBLISHER_TYPE`             | The message publisher to use (`kafka`, `rabbitmq`, `logging`) | `kafka`         |
| `outbox.publisher.default-topic`| `OUTBOX_PUBLISHER_DEFAULT_TOPIC`    | Default topic or queue for messages.                | `outbox-events` |
| `outbox.publisher.timeout`      | `OUTBOX_PUBLISHER_TIMEOUT`          | Timeout for publishing operations.                  | `PT30S`         |

**Kafka:**
```properties
spring.kafka.bootstrap-servers=localhost:9092
```

**RabbitMQ:**
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin
```

### Processing Configuration

Control how the outbox processor handles events.

| Property                        | Environment Variable                 | Description                                    | Default   |
| ------------------------------- | ------------------------------------ | ---------------------------------------------- | --------- |
| `outbox.processing.batch-size`  | `OUTBOX_PROCESSING_BATCH_SIZE`       | Max number of messages to process per run.     | `100`     |
| `outbox.processing.publish-rate`| `OUTBOX_PROCESSING_PUBLISH_RATE`     | Frequency of the outbox processing job.        | `PT10S`   |
| `outbox.processing.lock-timeout`| `OUTBOX_PROCESSING_LOCK_TIMEOUT`     | How long to lock an event during processing.   | `PT30S`   |

### Retry Configuration

Configure the retry mechanism for failed publishing attempts.

| Property                     | Environment Variable              | Description                               | Default |
| ---------------------------- | --------------------------------- | ----------------------------------------- | ------- |
| `outbox.retry.max-retries`   | `OUTBOX_RETRY_MAX_RETRIES`        | Max number of retry attempts per message. | `5`     |
| `outbox.retry.initial-delay` | `OUTBOX_RETRY_INITIAL_DELAY`      | Initial delay before the first retry.     | `PT1M`  |

## API Endpoints

Use the following endpoints to interact with the service and create outbox events.

### Create an Unordered Message

Publishes a message without guaranteeing the order of delivery.

-   **URL**: `/api/demo/unordered-message`
-   **Method**: `POST`
-   **Query Parameters**:
    -   `topic` (optional, default: `demo-topic`): The destination topic or queue.
    -   `aggregateId` (required): A unique identifier for the aggregate root (e.g., a user ID).
    -   `message` (required): The message content.
    -   `headers` (required): A comma-separated list of key-value pairs (e.g., `key1=value1,key2=value2`).
    -   `retryable` (optional, default: `false`): Whether the message should be retried on failure.

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/demo/unordered-message?topic=user-events&aggregateId=user-123&message=User updated profile&headers=source=api,correlationId=abc-123&retryable=true"
```

### Create an Ordered Message

Publishes a message while preserving the order of delivery based on the `aggregateId`. Messages with the same `aggregateId` are processed sequentially.

-   **URL**: `/api/demo/ordered-message`
-   **Method**: `POST`
-   **Query Parameters**:
    -   `topic` (optional, default: `demo-topic`): The destination topic or queue.
    -   `aggregateId` (required): The identifier for the aggregate to ensure ordering.
    -   `message` (required): The message content.
    -   `headers` (required): A comma-separated list of key-value pairs.
    -   `retryable` (optional, default: `false`): Whether the message should be retried on failure.

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/demo/ordered-message?topic=order-events&aggregateId=order-456&message=Order created&headers=source=api,correlationId=xyz-789&retryable=true"
```

## Running Tests

The project includes a comprehensive test suite covering unit and integration tests.

-   **Run All Tests**:
    ```bash
    ./gradlew test
    ```
-   **Run Tests with Coverage Report**:
    This command generates a JaCoCo test coverage report in `build/reports/jacoco/test/html/index.html`.
    ```bash

    ./gradlew test jacocoTestReport
    ```

## Docker Services

The included `docker-compose.yml` provides the following services:

| Service               | Description                           | URL                                         | Credentials     |
| --------------------- | ------------------------------------- | ------------------------------------------- | --------------- |
| **MySQL**             | Database for the `outbox` table.      | `jdbc:mysql://localhost:3306/outboxdb`      | `user`/`password` |
| **Kafka & Zookeeper** | Message streaming platform.           | `localhost:9092`                            | -               |
| **Kafka UI**          | Web interface for managing Kafka.     | [http://localhost:8081](http://localhost:8081) | -               |
| **RabbitMQ**          | Message broker with management UI.    | [http://localhost:15672](http://localhost:15672) | `admin`/`admin`   |

## Contributing

Contributions are welcome! Please follow these steps:

1.  Fork the repository.
2.  Create a new feature branch (`git checkout -b feature/your-feature`).
3.  Make your changes and add tests.
4.  Ensure all tests pass (`./gradlew test`).
5.  Submit a pull request with a clear description of your changes.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
