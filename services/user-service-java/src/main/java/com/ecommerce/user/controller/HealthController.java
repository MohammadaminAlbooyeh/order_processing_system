package com.ecommerce.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> dbStatus = Map.of("status", "unhealthy", "type", "postgresql");
        try {
            jdbcTemplate.execute("SELECT 1");
            dbStatus = Map.of("status", "healthy", "type", "postgresql");
        } catch (Exception e) {
            dbStatus = Map.of("status", "unhealthy", "type", "postgresql", "error", e.getMessage());
        }
        return Map.of(
            "status", "healthy",
            "timestamp", Instant.now().toString()
        );
    }
}
