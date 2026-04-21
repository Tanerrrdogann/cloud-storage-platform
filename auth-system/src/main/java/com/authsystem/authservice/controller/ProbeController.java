package com.authsystem.authservice.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ProbeController {

    private final DataSource dataSource;
    private final RabbitTemplate rabbitTemplate;

    public ProbeController(DataSource dataSource, RabbitTemplate rabbitTemplate) {
        this.dataSource = dataSource;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean dbUp = checkDatabase();
        boolean rabbitUp = checkRabbit();

        checks.put("database", dbUp ? "UP" : "DOWN");
        checks.put("broker", rabbitUp ? "UP" : "DOWN");

        boolean ready = dbUp && rabbitUp;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ready ? "UP" : "DOWN");
        body.put("checks", checks);

        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean checkRabbit() {
        try {
            return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> channel.isOpen()));
        } catch (Exception ex) {
            return false;
        }
    }
}
