package com.ecommerce.order.service;

import com.ecommerce.order.repository.UserLookupRepository;
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
    private final UserLookupRepository userLookupRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventPublisherService(StringRedisTemplate redisTemplate, UserLookupRepository userLookupRepository) {
        this.redisTemplate = redisTemplate;
        this.userLookupRepository = userLookupRepository;
    }

    public void publishOrderCreated(Long orderId, Long userId) {
        Map<String, Object> event = baseEvent("ORDER_CREATED", orderId, userId);
        push(ORDER_QUEUE, event, "ORDER_CREATED", orderId);
    }

    public void publishOrderCancelled(Long orderId, Long userId) {
        Map<String, Object> event = baseEvent("ORDER_CANCELLED", orderId, userId);
        push(NOTIFICATION_QUEUE, event, "ORDER_CANCELLED", orderId);
    }

    private Map<String, Object> baseEvent(String type, Long orderId, Long userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("order_id", orderId);
        event.put("user_id", userId);
        Map<String, String> contact = userLookupRepository.findContact(userId);
        event.put("user_email", contact.getOrDefault("email", ""));
        event.put("user_phone", contact.getOrDefault("phone", ""));
        return event;
    }

    private void push(String queue, Map<String, Object> event, String type, Long orderId) {
        try {
            redisTemplate.opsForList().leftPush(queue, objectMapper.writeValueAsString(event));
            logger.info("Published {} event for order {}", type, orderId);
        } catch (Exception e) {
            logger.error("Failed to publish {} event: {}", type, e.getMessage());
        }
    }
}
