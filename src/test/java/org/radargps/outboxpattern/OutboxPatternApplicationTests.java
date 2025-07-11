package org.radargps.outboxpattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.radargps.outboxpattern.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
class OutboxPatternApplicationTests {

    @Autowired
    OutboxService outboxService;;

    @Test
    void contextLoads() {
    }

    @Test
    void processPendingMessages() {
        outboxService.processPendingMessage();
    }

    @Test
    void createMessage() throws JsonProcessingException {
        int randNumber = ThreadLocalRandom.current().nextInt(1000);
        var messageType = "ORDER_CREATED";
        var messagePayload = "{'order': " + randNumber + ",  'status': 'created'}";

        outboxService.createMessage(messageType, messagePayload);
    }



}
