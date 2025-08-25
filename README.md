# 📦 Outbox Pattern Library

A beautifully simple, production-ready **Transactional Outbox Pattern** library for Spring Boot applications.

## ✨ Core Features

- **🎯 Single Entry Point** - One simple `OutboxManager` interface
- **🔄 Automatic Processing** - Background processing with retry logic
- **📊 Built-in Monitoring** - Health checks and metrics
- **🔌 Multiple Brokers** - Kafka, RabbitMQ, or logging
- **⚡ High Performance** - Optimized queries and concurrency
- **📦 Zero Configuration** - Works with sensible defaults

## 🚀 Quick Start

### 1. Add Dependency
```gradle
implementation 'com.github.mahdim1000:outbox-pattern-library:1.0.0'
```

### 2. Use the OutboxManager
```java
@Service
@Transactional
public class OrderService {
    
    private final OutboxManager outboxManager;
    
    public void createOrder(String customerId, BigDecimal amount) {
        // 1. Save your business data
        Order order = saveOrder(customerId, amount);
        
        // 2. Publish event - that's it! 🎉
        outboxManager.publish("order.created", order.getId(), 
                new OrderCreatedEvent(order.getId(), customerId, amount))
                     .execute();
    }
}
```

## 🎯 API Examples

```java
// Simple unordered message
outboxManager.publish("user.created", userId, userEvent)
             .execute();

// Ordered message with headers  
outboxManager.publishOrdered("order.events", orderId, orderEvent)
             .withHeader("version", "1.0")
             .retryable(true)
             .execute();

// Health check
boolean healthy = outboxManager.isHealthy();
OutboxMetrics metrics = outboxManager.getMetrics();
```

## ⚙️ Configuration

```yaml
outbox:
  processing:
    enabled: true
    batch-size: 100
    publish-rate: PT10S
  publisher:
    type: logging  # or kafka, rabbitmq
```

This library follows **KISS principle** - maximum power with minimal complexity.
