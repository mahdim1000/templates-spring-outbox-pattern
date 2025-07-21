package com.github.mahdim1000.outboxpattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mahdim1000.outboxpattern.outbox.Outbox;
import com.github.mahdim1000.outboxpattern.outbox.OutboxRepository;
import com.github.mahdim1000.outboxpattern.outbox.OutboxService;
import com.github.mahdim1000.outboxpattern.publisher.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OutboxPatternApplicationTests {

    @Autowired
    private OutboxService outboxService;
    
    @Autowired
    private OutboxRepository outboxRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        // Application context loads successfully
        assertThat(outboxService).isNotNull();
        assertThat(outboxRepository).isNotNull();
        assertThat(eventPublisher).isNotNull();
        assertThat(eventPublisher.getPublisherType()).isEqualTo("logging");
    }

}
