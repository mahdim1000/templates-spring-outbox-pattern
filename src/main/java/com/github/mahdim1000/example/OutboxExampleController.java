package com.github.mahdim1000.example;

import com.github.mahdim1000.core.OutboxManager;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Example REST controller showing how to integrate the Outbox Pattern library.
 * 
 * This demonstrates:
 * - Simple usage in REST endpoints
 * - Health monitoring
 * - Metrics collection
 */
@RestController
@RequestMapping("/api/outbox-example")
public class OutboxExampleController {
    
    private final OrderService orderService;
    private final OutboxManager outboxManager;
    
    public OutboxExampleController(OrderService orderService, OutboxManager outboxManager) {
        this.orderService = orderService;
        this.outboxManager = outboxManager;
    }
    
    @PostMapping("/orders")
    public Map<String, String> createOrder(@RequestParam String customerId,
                                          @RequestParam BigDecimal amount) {
        String orderId = orderService.createOrder(customerId, amount);
        return Map.of("orderId", orderId, "status", "created");
    }
    
    @PutMapping("/orders/{orderId}")
    public Map<String, String> updateOrder(@PathVariable String orderId,
                                          @RequestParam BigDecimal amount) {
        orderService.updateOrder(orderId, amount);
        return Map.of("orderId", orderId, "status", "updated");
    }
    
    @DeleteMapping("/orders/{orderId}")
    public Map<String, String> cancelOrder(@PathVariable String orderId,
                                          @RequestParam String reason) {
        orderService.cancelOrder(orderId, reason);
        return Map.of("orderId", orderId, "status", "cancelled");
    }
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "healthy", outboxManager.isHealthy(),
            "metrics", outboxManager.getMetrics()
        );
    }
    
    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        var metrics = outboxManager.getMetrics();
        return Map.of(
            "pending", metrics.pendingCount(),
            "failed", metrics.failedCount(),
            "published", metrics.publishedCount(),
            "deadLetter", metrics.deadLetterCount(),
            "total", metrics.totalCount(),
            "successRate", String.format("%.2f%%", metrics.successRate())
        );
    }
}
