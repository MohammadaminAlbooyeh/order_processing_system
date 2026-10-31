package com.ecommerce.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** Publishes events onto "notification_queue", the same Redis list notification-service consumes. */
@Service
public class NotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPublisher.class);
    private static final String QUEUE_KEY = "notification_queue";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publishEvent(String eventType, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>(data);
        payload.put("type", eventType);
        try {
            redisTemplate.opsForList().leftPush(QUEUE_KEY, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            logger.error("Failed to publish {} event: {}", eventType, e.getMessage(), e);
        }
    }

    public void sendOutOfStockNotification(long userId, String userEmail, String userPhone, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("user_email", userEmail);
        data.put("user_phone", userPhone);
        data.put("message", message);
        publishEvent("OUT_OF_STOCK", data);
    }
}
