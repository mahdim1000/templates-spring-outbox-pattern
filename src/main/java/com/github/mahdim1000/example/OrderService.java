package com.github.mahdim1000.example;

import com.github.mahdim1000.core.OutboxManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Example service demonstrating how to use the Outbox Pattern library.
 * 
 * This shows the typical usage pattern:
 * 1. Perform business operations within a transaction
 * 2. Use OutboxManager to publish events within the same transaction
 * 3. Events are automatically processed asynchronously
 */
@Service
public class OrderService {
    
    private final OutboxManager outboxManager;
    
    public OrderService(OutboxManager outboxManager) {
        this.outboxManager = outboxManager;
    }
    
    /**
     * Creates an order and publishes an OrderCreated event.
     * The event will only be published if the order creation succeeds.
     */
    @Transactional
    public String createOrder(String customerId, BigDecimal amount) {
        String orderId = UUID.randomUUID().toString();
        
        // 1. Perform business logic (save to database, etc.)
        saveOrder(orderId, customerId, amount);
        
        // 2. Publish event using the outbox pattern
        var orderCreatedEvent = new OrderCreatedEvent(
            orderId, 
            customerId, 
            amount, 
            LocalDateTime.now()
        );
        outboxManager.publish("order.created", orderId, orderCreatedEvent)
                     .withHeader("eventType", "OrderCreated")
                     .withHeader("version", "1.0")
                     .execute();
        
        return orderId;
    }
    
    /**
     * Updates an order and publishes an OrderUpdated event.
     * Uses ordered messaging to ensure events are processed in sequence.
     */
    @Transactional
    public void updateOrder(String orderId, BigDecimal newAmount) {
        // 1. Perform business logic
        updateOrderAmount(orderId, newAmount);
        
        // 2. Publish ordered event (events for same orderId will be processed in order)
        var orderUpdatedEvent = new OrderUpdatedEvent(
            orderId, 
            newAmount, 
            LocalDateTime.now()
        );
        
        outboxManager.publishOrdered("order.updated", orderId, orderUpdatedEvent)
                     .withHeaders(Map.of(
                         "eventType", "OrderUpdated",
                         "version", "1.0"
                     ))
                     .retryable(true)
                     .execute();
    }
    
    /**
     * Cancels an order with non-retryable event.
     * If the event fails to publish, it won't be retried.
     */
    @Transactional
    public void cancelOrder(String orderId, String reason) {
        // 1. Perform business logic
        markOrderAsCancelled(orderId, reason);
        
        // 2. Publish non-retryable event
        var orderCancelledEvent = new OrderCancelledEvent(
            orderId, 
            reason, 
            LocalDateTime.now()
        );
        
        outboxManager.publishOrdered("order.cancelled", orderId, orderCancelledEvent)
                     .withHeader("eventType", "OrderCancelled")
                     .retryable(false)  // Don't retry if publishing fails
                     .execute();
    }
    
    // Simulate business operations
    private void saveOrder(String orderId, String customerId, BigDecimal amount) {
        // In a real application, this would save to the database
        System.out.println("💾 Saved order: " + orderId);
    }
    
    private void updateOrderAmount(String orderId, BigDecimal newAmount) {
        // In a real application, this would update the database
        System.out.println("💾 Updated order " + orderId + " amount to: " + newAmount);
    }
    
    private void markOrderAsCancelled(String orderId, String reason) {
        // In a real application, this would update the database
        System.out.println("💾 Cancelled order " + orderId + " reason: " + reason);
    }
    
    // Event DTOs
    public record OrderCreatedEvent(
        String orderId,
        String customerId,
        BigDecimal amount,
        LocalDateTime timestamp
    ) {}
    
    public record OrderUpdatedEvent(
        String orderId,
        BigDecimal newAmount,
        LocalDateTime timestamp
    ) {}
    
    public record OrderCancelledEvent(
        String orderId,
        String reason,
        LocalDateTime timestamp
    ) {}
}
