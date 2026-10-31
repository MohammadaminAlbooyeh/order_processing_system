package com.ecommerce.worker.service;

import com.ecommerce.worker.model.Order;
import com.ecommerce.worker.model.OrderItem;
import com.ecommerce.worker.repository.OrderRepository;
import com.ecommerce.worker.util.ProductCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Java port of process_order from the Python worker's tasks.py. */
@Service
public class OrderProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);
    private static final double BASE_SHIPPING_RATE = 5.0;
    private static final double PER_KG_RATE = 2.0;
    private static final int MAX_RECOMMENDATIONS = 5;

    private final OrderRepository orderRepository;
    private final NotificationPublisher notificationPublisher;
    private final RecommendationService recommendationService;

    public OrderProcessingService(OrderRepository orderRepository,
                                  NotificationPublisher notificationPublisher,
                                  RecommendationService recommendationService) {
        this.orderRepository = orderRepository;
        this.notificationPublisher = notificationPublisher;
        this.recommendationService = recommendationService;
    }

    public void processOrder(long orderId, String userEmail, String userPhone) {
        try {
            logger.info("Processing order: {}", orderId);
            Optional<Order> maybeOrder = orderRepository.findById(orderId);
            if (maybeOrder.isEmpty()) {
                logger.warn("Order not found: {}", orderId);
                return;
            }
            Order order = maybeOrder.get();

            double shippingCost = calculateShipping(order.destination(), order.weight());
            logger.info("Shipping cost for order {}: ${}", orderId, String.format("%.2f", shippingCost));

            for (OrderItem item : order.items()) {
                if (!ProductCatalog.hasInventory(item.productId(), item.quantity())) {
                    logger.warn("Inventory check failed for order {}, item {}", orderId, item.productId());
                    notificationPublisher.sendOutOfStockNotification(
                            order.userId(), userEmail, userPhone,
                            "Item " + item.productId() + " out of stock");
                    return;
                }
            }

            List<Integer> recommendations = recommendProducts(order.items());

            orderRepository.updateStatus(orderId, "processing");
            logger.info("Order {} status updated to processing", orderId);

            Map<String, Object> event = new HashMap<>();
            event.put("order_id", orderId);
            event.put("user_id", order.userId());
            event.put("user_email", userEmail);
            event.put("user_phone", userPhone);
            event.put("recommendations", recommendations);
            notificationPublisher.publishEvent("ORDER_PROCESSED", event);
            logger.info("Order {} processed successfully", orderId);

            // Refresh this user's cached product recommendations off the back of the new order.
            recommendationService.calculateRecommendations(order.userId());
        } catch (Exception e) {
            logger.error("Error processing order {}: {}", orderId, e.getMessage(), e);
        }
    }

    double calculateShipping(String destination, double weight) {
        return BASE_SHIPPING_RATE + (weight * PER_KG_RATE);
    }

    List<Integer> recommendProducts(List<OrderItem> items) {
        List<Integer> productIds = items.stream().map(OrderItem::productId).toList();
        return ProductCatalog.recommendationsFor(productIds, MAX_RECOMMENDATIONS);
    }
}
