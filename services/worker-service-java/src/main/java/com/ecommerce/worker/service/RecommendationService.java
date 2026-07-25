package com.ecommerce.worker.service;

import com.ecommerce.worker.model.OrderItem;
import com.ecommerce.worker.repository.OrderRepository;
import com.ecommerce.worker.util.ProductCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java port of calculate_recommendations / MockMLModel from the Python worker.
 * Note: nothing in this system currently triggers this flow automatically
 * (same gap exists in the original Python worker-service) - it is exposed
 * here as a directly callable service, same as the Python version.
 */
@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final String RECOMMENDATIONS_KEY_PREFIX = "user_recommendations:";
    private static final Duration RECOMMENDATIONS_TTL = Duration.ofSeconds(86400);
    private static final int RECENT_ORDERS_LIMIT = 10;
    private static final int TOP_PRODUCTS_LIMIT = 3;
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final List<Integer> DEFAULT_RECOMMENDATIONS = List.of(10, 11, 12);

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecommendationService(OrderRepository orderRepository, StringRedisTemplate redisTemplate) {
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
    }

    public void calculateRecommendations(long userId) {
        try {
            logger.info("Calculating recommendations for user: {}", userId);
            List<OrderItem> history = getUserPurchaseHistory(userId);
            List<Integer> recommendations = predict(history);
            saveRecommendations(userId, recommendations);
            logger.info("Recommendations calculated and saved for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error calculating recommendations for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private List<OrderItem> getUserPurchaseHistory(long userId) {
        try {
            return orderRepository.findRecentItemsByUser(userId, RECENT_ORDERS_LIMIT);
        } catch (Exception e) {
            logger.error("Error fetching purchase history for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    List<Integer> predict(List<OrderItem> history) {
        if (history.isEmpty()) {
            return DEFAULT_RECOMMENDATIONS;
        }

        Map<Integer, Integer> productCounts = new HashMap<>();
        for (OrderItem item : history) {
            productCounts.merge(item.productId(), 1, Integer::sum);
        }

        List<Integer> topProducts = productCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(Map.Entry::getKey)
                .toList();

        return ProductCatalog.recommendationsFor(topProducts, MAX_RECOMMENDATIONS);
    }

    private void saveRecommendations(long userId, List<Integer> recommendations) {
        try {
            String key = RECOMMENDATIONS_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(recommendations), RECOMMENDATIONS_TTL);
            logger.info("Saved {} recommendations for user {}", recommendations.size(), userId);
        } catch (Exception e) {
            logger.error("Error saving recommendations for user {}: {}", userId, e.getMessage());
        }
    }
}
