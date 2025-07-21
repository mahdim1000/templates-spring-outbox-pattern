package com.github.mahdim1000.outboxpattern;

import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    public void publish(String topic, Object event) {
        System.out.println("Publishing event " + event + " to topic " + topic);
//        throw new IllegalArgumentException("not implemented yet");
    }
}
