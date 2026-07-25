package com.ecommerce.worker.listener;

import com.ecommerce.worker.service.OrderProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls "order_queue", the Redis list order-api's create_order endpoint pushes
 * ORDER_CREATED events onto (services/order-api/routes.py). Nothing in the
 * original Python system consumed this list - worker-service only defined
 * Celery tasks that were never dispatched. This listener closes that gap.
 */
@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);
    private static final String QUEUE_KEY = "order_queue";
    private static final String ORDER_CREATED_EVENT = "ORDER_CREATED";

    private final StringRedisTemplate redisTemplate;
    private final OrderProcessingService orderProcessingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventListener(StringRedisTemplate redisTemplate, OrderProcessingService orderProcessingService) {
        this.redisTemplate = redisTemplate;
        this.orderProcessingService = orderProcessingService;
    }

    @Scheduled(fixedDelay = 100)
    public void pollQueue() {
        String rawEvent = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        if (rawEvent == null) {
            return;
        }
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String eventType = event.path("event").asText("");
            logger.info("Received order event: {}", eventType);
            if (ORDER_CREATED_EVENT.equals(eventType)) {
                orderProcessingService.processOrder(event.path("order_id").asLong());
            }
        } catch (Exception e) {
            logger.error("Error processing order event: {}", e.getMessage(), e);
        }
    }
}
