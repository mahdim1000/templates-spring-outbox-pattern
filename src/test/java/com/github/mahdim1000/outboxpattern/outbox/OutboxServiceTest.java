package com.github.mahdim1000.outboxpattern.outbox;

import com.github.mahdim1000.outboxpattern.publisher.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxServiceTest {

    @Autowired
    private OutboxService outboxService;


} 