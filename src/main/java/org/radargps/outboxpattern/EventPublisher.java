package org.radargps.outboxpattern;

import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    public void publish(String topic, Object event) {
        System.out.println("Publishing event " + event + " to topic " + topic);
    }
}
