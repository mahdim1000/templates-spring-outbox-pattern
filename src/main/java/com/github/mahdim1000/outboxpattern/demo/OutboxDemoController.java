package com.github.mahdim1000.outboxpattern.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.mahdim1000.outboxpattern.outbox.OutboxService;
import com.github.mahdim1000.outboxpattern.publisher.EventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Demo controller to test the outbox pattern with different message publishers
 */
@RestController
@RequestMapping("/api/demo")
public class OutboxDemoController {

    private final OutboxService outboxService;
    private final EventPublisher eventPublisher;

    public OutboxDemoController(OutboxService outboxService, EventPublisher eventPublisher) {
        this.outboxService = outboxService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/unordered-message")
    public ResponseEntity<Map<String, Object>> createUnorderedMessage(
            @RequestParam(defaultValue = "demo-topic") String topic,
            @RequestParam String aggregateId,
            @RequestParam String message,
            @RequestParam("headers") String strHeader,
            @RequestParam boolean retryable) throws JsonProcessingException {

        Map<String, String> headers = Arrays.stream(strHeader.split(","))
                .map(kv -> kv.split("="))
                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
        Map<String, Object> payload = Map.of(
            "message", message,
            "timestamp", LocalDateTime.now().toString(),
            "type", "DemoEvent"
        );

        outboxService.createUnOrderedMessage(topic, aggregateId, payload, headers, retryable);
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/ordered-message")
    public ResponseEntity<Map<String, Object>> createOrderedMessage(
            @RequestParam(defaultValue = "demo-topic") String topic,
            @RequestParam String aggregateId,
            @RequestParam String message,
            @RequestParam("headers") String strHeader,
            @RequestParam boolean retryable) throws JsonProcessingException {

        Map<String, String> headers = Arrays.stream(strHeader.split(","))
                .map(kv -> kv.split("="))
                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
        Map<String, Object> payload = Map.of(
                "message", message,
                "timestamp", LocalDateTime.now().toString(),
                "type", "DemoEvent"
        );

        outboxService.createOrderedMessage(topic, aggregateId, payload, headers, retryable);
        return ResponseEntity.ok(payload);
    }
} 