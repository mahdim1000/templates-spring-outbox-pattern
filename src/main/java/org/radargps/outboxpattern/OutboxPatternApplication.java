package org.radargps.outboxpattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OutboxPatternApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutboxPatternApplication.class, args);
    }

}
