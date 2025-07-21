# Event Versioning for Outbox Pattern

## Overview

This implementation introduces reliable event versioning to the outbox pattern to ensure ordered event processing. Events for the same aggregate are processed sequentially, with later versions waiting for earlier versions to be successfully published.

## Key Features

### 1. Automatic Version Assignment
- Events for each aggregate are automatically assigned sequential version numbers starting from 1
- Version numbers are generated based on the maximum existing version for that aggregate

### 2. Ordered Processing
- Events are only processed if all earlier versions for the same aggregate have been successfully published
- This ensures strict ordering within each aggregate while allowing parallel processing across different aggregates

### 3. Version-Aware Repository Queries
The repository queries have been enhanced to respect version ordering:

```sql
-- Only select events where no earlier versions are unpublished
SELECT * FROM Outbox o
WHERE o.status = 'PENDING'
AND NOT EXISTS (
    SELECT 1 FROM Outbox o2 
    WHERE o2.aggregateId = o.aggregateId 
    AND o2.version < o.version 
    AND o2.status != 'PUBLISHED'
)
ORDER BY o.aggregateId ASC, o.version ASC
```

### 4. Explicit Version Control
For scenarios requiring explicit version control, use the `createOrderedMessage` method:

```java
// This will only succeed if it's the next expected version
outboxService.createOrderedMessage(topic, aggregateId, payload, expectedVersion);
```

## API Usage

### Standard Message Creation (Automatic Versioning)
```java
outboxService.createMessage("ORDER_EVENTS", "order-123", orderCreatedEvent);
outboxService.createMessage("ORDER_EVENTS", "order-123", orderConfirmedEvent);
// Versions 1 and 2 are automatically assigned and processed in order
```

### Ordered Message Creation (Explicit Version Control)
```java
// Get the next expected version
Integer nextVersion = outboxService.getNextExpectedVersion("order-123");

// Create message with specific version validation
outboxService.createOrderedMessage("ORDER_EVENTS", "order-123", event, nextVersion);
```

### Version Validation
```java
// Check if a version can be processed
boolean canProcess = outboxService.canProcessVersion("order-123", 2);
```

## Processing Behavior

### Single Aggregate Processing
For events with the same `aggregateId`:
1. Version 1 must be published before Version 2 can be processed
2. Version 2 must be published before Version 3 can be processed
3. And so on...

### Multi-Aggregate Processing
Events from different aggregates are processed independently:
- `order-123` version 2 can be processed even if `order-456` version 1 failed
- This maintains performance while ensuring per-aggregate ordering

### Failure Handling
If an earlier version fails:
- Later versions for the same aggregate are blocked
- The failed event will be retried according to the retry policy
- Once the failed event succeeds, later versions become available for processing

## Database Schema

The `Outbox` table includes:
- `aggregateId`: Groups related events
- `version`: Sequential version number within each aggregate
- Standard outbox fields: `status`, `retryCount`, `createdAt`, etc.

## Best Practices

1. **Use Meaningful Aggregate IDs**: Group related events that need ordering
2. **Handle Version Gaps**: The system prevents version gaps automatically
3. **Monitor Processing**: Use the utility methods to check processing status
4. **Design for Idempotency**: Events may be retried, so ensure idempotent handling

## Example Scenario

```java
// Order lifecycle events - these will be processed in order
outboxService.createMessage("ORDER_EVENTS", "order-123", orderCreated);    // Version 1
outboxService.createMessage("ORDER_EVENTS", "order-123", paymentTaken);    // Version 2
outboxService.createMessage("ORDER_EVENTS", "order-123", orderShipped);    // Version 3

// If version 1 fails, versions 2 and 3 will wait
// Once version 1 succeeds, version 2 will be processed
// Once version 2 succeeds, version 3 will be processed
```

This ensures that downstream consumers receive events in the correct order, maintaining data consistency across the distributed system. 