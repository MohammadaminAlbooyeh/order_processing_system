package com.ecommerce.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EventPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);
    private static final String ORDER_QUEUE = "order_queue";
    private static final String NOTIFICATION_QUEUE = "notification_queue";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventPublisherService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publishOrderCreated(Long orderId, Long userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("event", "ORDER_CREATED");
        event.put("order_id", orderId);
        event.put("user_id", userId);
        try {
            redisTemplate.opsForList().leftPush(ORDER_QUEUE, objectMapper.writeValueAsString(event));
            logger.info("Published ORDER_CREATED event for order {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_CREATED event: {}", e.getMessage());
        }
    }

    public void publishOrderCancelled(Long orderId, Long userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "ORDER_CANCELLED");
        event.put("order_id", orderId);
        event.put("user_id", userId);
        try {
            redisTemplate.opsForList().leftPush(NOTIFICATION_QUEUE, objectMapper.writeValueAsString(event));
            logger.info("Published ORDER_CANCELLED event for order {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_CANCELLED event: {}", e.getMessage());
        }
    }
}
