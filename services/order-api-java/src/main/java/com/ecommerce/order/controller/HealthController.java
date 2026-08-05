package com.ecommerce.order.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    public HealthController(DataSource dataSource, org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> dbStatus = Map.of("status", "unhealthy", "type", "postgresql");
        Map<String, Object> redisStatus = Map.of("status", "unhealthy", "type", "redis");

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                dbStatus = Map.of("status", "healthy", "type", "postgresql");
            }
        } catch (SQLException e) {
            dbStatus = Map.of("status", "unhealthy", "type", "postgresql", "error", e.getMessage());
        }

        try {
            redisTemplate.opsForValue().get("health-check");
            redisStatus = Map.of("status", "healthy", "type", "redis");
        } catch (Exception e) {
            redisStatus = Map.of("status", "unhealthy", "type", "redis", "error", e.getMessage());
        }

        String overallStatus = (dbStatus.get("status").equals("unhealthy") || redisStatus.get("status").equals("unhealthy"))
            ? "unhealthy" : "healthy";

        return Map.of(
            "status", overallStatus,
            "timestamp", Instant.now().toString(),
            "dependencies", Map.of("database", dbStatus, "redis", redisStatus)
        );
    }
}
