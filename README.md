# Outbox Pattern Implementation

A Spring Boot application demonstrating the **Outbox Pattern** for reliable message publishing and distributed system consistency.

## 🎯 Overview

The Outbox Pattern is a microservices design pattern that ensures reliable message publishing by storing events in a local database table (the "outbox") within the same transaction as business data changes. This guarantees that either both the business operation and the event are persisted, or neither are, maintaining data consistency.

## 🏗️ Architecture

This implementation provides:

- **Transactional Outbox**: Events are stored in the same database transaction as business operations
- **Scheduled Processing**: Background service processes outbox events and publishes them
- **Reliable Delivery**: Ensures messages are eventually published even if the initial attempt fails
- **Spring Boot Integration**: Leverages Spring's scheduling and transaction management

## 🚀 Features

- ✅ Transactional event storage
- ✅ Automatic event processing with scheduling
- ✅ JPA/Hibernate integration
- ✅ MySQL database support
- ✅ Comprehensive test coverage
- ✅ Production-ready configuration

## 📋 Prerequisites

- Java 17 or higher
- MySQL 8.0+
- Gradle 7.0+

## 🛠️ Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd outbox-pattern
   ```

2. **Configure database**

   Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/outbox_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

## 🧪 Testing

Run the test suite:

```bash
./gradlew test
```

Run specific test class:

```bash
./gradlew test --tests OutboxPatternApplicationTests
```