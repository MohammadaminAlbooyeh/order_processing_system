package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderItem;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;
    private final EventPublisherService eventPublisherService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of(
            "pending", List.of("processing", "cancelled"),
            "processing", List.of("shipped", "cancelled"),
            "shipped", List.of("delivered", "cancelled"),
            "delivered", List.of(),
            "cancelled", List.of()
    );

    public OrderService(OrderRepository orderRepository, StringRedisTemplate redisTemplate, EventPublisherService eventPublisherService) {
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisherService = eventPublisherService;
    }

    public OrderResponse createOrder(Long userId, String destination, Double weight, List<OrderItem> items) throws Exception {
        Order order = new Order();
        order.setUserId(userId);
        order.setDestination(destination);
        order.setWeight(weight);
        order.setStatus("pending");
        order.setItems(objectMapper.writeValueAsString(items));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Insert manually using JdbcTemplate since JPA might not work well with our DataSource bean
        Long orderId = orderRepository.createOrder(order);

        order.setId(orderId);
        eventPublisherService.publishOrderCreated(orderId, userId);
        logger.info("Order {} created successfully", orderId);
        return toResponse(order, items);
    }

    public Optional<OrderResponse> getOrder(Long orderId, Long requesterId) {
        String cacheKey = "order:" + orderId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            logger.info("Cache hit for order {}", orderId);
            try {
                OrderResponse response = objectMapper.readValue(cached, OrderResponse.class);
                if (!response.getUserId().equals(requesterId)) {
                    logger.warn("User {} attempted to access order {} owned by user {}", requesterId, orderId, response.getUserId());
                    return Optional.empty();
                }
                return Optional.of(response);
            } catch (Exception e) {
                logger.warn("Failed to parse cached order: {}", e.getMessage());
            }
        }

        logger.info("Cache miss for order {}, querying database", orderId);
        Optional<Order> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            logger.warn("Order {} not found", orderId);
            return Optional.empty();
        }

        Order order = maybeOrder.get();
        if (!order.getUserId().equals(requesterId)) {
            logger.warn("User {} attempted to access order {} owned by user {}", requesterId, orderId, order.getUserId());
            return Optional.empty();
        }

        List<OrderItem> items = orderRepository.parseItems(order.getItems());
        OrderResponse response = toResponse(order, items);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.warn("Failed to cache order: {}", e.getMessage());
        }
        return Optional.of(response);
    }

    public List<OrderResponse> listOrders(Long userId, String status, int limit, int offset) {
        List<Order> orders = orderRepository.findByUserId(userId, status, limit, offset);
        return orders.stream().map(order -> {
            List<OrderItem> items = orderRepository.parseItems(order.getItems());
            return toResponse(order, items);
        }).toList();
    }

    public long countOrders(Long userId, String status) {
        return orderRepository.countByUserId(userId, status);
    }

    public Optional<OrderResponse> updateOrder(Long orderId, Long requesterId, String newStatus, String newDestination) {
        Optional<Order> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            logger.warn("Order {} not found", orderId);
            return Optional.empty();
        }

        Order order = maybeOrder.get();
        if (!order.getUserId().equals(requesterId)) {
            logger.warn("User {} attempted to update order {} owned by user {}", requesterId, orderId, order.getUserId());
            return Optional.empty();
        }

        if (newStatus != null) {
            List<String> transitions = VALID_TRANSITIONS.getOrDefault(order.getStatus(), List.of());
            if (!transitions.contains(newStatus)) {
                logger.warn("Invalid status transition for order {}: {} -> {}", orderId, order.getStatus(), newStatus);
                throw new IllegalArgumentException("Cannot transition from " + order.getStatus() + " to " + newStatus);
            }
            order.setStatus(newStatus);
        }

        if (newDestination != null) {
            order.setDestination(newDestination);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.update(order);
        logger.info("Order {} updated successfully", orderId);

        String cacheKey = "order:" + orderId;
        redisTemplate.delete(cacheKey);

        List<OrderItem> items = orderRepository.parseItems(order.getItems());
        return Optional.of(toResponse(order, items));
    }

    public boolean cancelOrder(Long orderId, Long requesterId) {
        Optional<Order> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            logger.warn("Order {} not found", orderId);
            return false;
        }

        Order order = maybeOrder.get();
        if (!order.getUserId().equals(requesterId)) {
            logger.warn("User {} attempted to cancel order {} owned by user {}", requesterId, orderId, order.getUserId());
            return false;
        }

        if (List.of("shipped", "delivered", "cancelled").contains(order.getStatus())) {
            logger.warn("Cannot cancel order {} with status {}", orderId, order.getStatus());
            throw new IllegalArgumentException("Cannot cancel order with status " + order.getStatus());
        }

        order.setStatus("cancelled");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.update(order);
        logger.info("Order {} cancelled successfully", orderId);

        String cacheKey = "order:" + orderId;
        redisTemplate.delete(cacheKey);

        eventPublisherService.publishOrderCancelled(orderId, order.getUserId());
        return true;
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setDestination(order.getDestination());
        response.setWeight(order.getWeight());
        response.setItems(items.stream().map(item -> Map.<String, Object>of(
                "product_id", item.getProductId(),
                "quantity", item.getQuantity()
        )).toList());
        response.setCreatedAt(order.getCreatedAt().toString());
        response.setUpdatedAt(order.getUpdatedAt().toString());
        return response;
    }
}
