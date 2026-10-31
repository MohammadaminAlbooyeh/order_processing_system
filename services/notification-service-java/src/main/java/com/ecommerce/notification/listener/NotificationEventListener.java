package com.ecommerce.notification.listener;

import com.ecommerce.notification.service.EmailSender;
import com.ecommerce.notification.service.SmsSender;
import com.ecommerce.notification.util.NotificationMessageFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the "notification_queue" Redis list, mirroring the polling loop used by the
 * original Python service (lpush on the producer side, lpop here). Every event
 * carries a "type" discriminator plus the recipient's contact details
 * ("user_email" / "user_phone") supplied by the producing service.
 */
@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final String QUEUE_KEY = "notification_queue";
    private static final String DEAD_LETTER_KEY = "notification_queue_dead";

    private final StringRedisTemplate redisTemplate;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationEventListener(StringRedisTemplate redisTemplate, EmailSender emailSender, SmsSender smsSender) {
        this.redisTemplate = redisTemplate;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    @Scheduled(fixedDelay = 100)
    public void pollQueue() {
        String rawEvent = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        if (rawEvent == null) {
            return;
        }
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String type = event.path("type").asText("");
            logger.info("Processing event: {}", type);
            switch (type) {
                case "ORDER_PROCESSED" -> handleOrderProcessed(event);
                case "ORDER_CANCELLED" -> handleOrderCancelled(event);
                case "OUT_OF_STOCK" -> handleOutOfStock(event);
                default -> logger.debug("No handler for event type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error processing notification event: {}", e.getMessage(), e);
            redisTemplate.opsForList().rightPush(DEAD_LETTER_KEY, rawEvent);
        }
    }

    private void handleOrderProcessed(JsonNode event) {
        long orderId = event.path("order_id").asLong();
        String email = event.path("user_email").asText("");
        String phone = event.path("user_phone").asText("");

        sendEmail(email,
                NotificationMessageFormatter.buildOrderConfirmationSubject(orderId),
                NotificationMessageFormatter.buildOrderConfirmationBody(orderId, event.path("recommendations")),
                orderId);
        sendSms(phone, NotificationMessageFormatter.buildTrackingSmsBody(orderId), orderId);
    }

    private void handleOrderCancelled(JsonNode event) {
        long orderId = event.path("order_id").asLong();
        sendEmail(event.path("user_email").asText(""),
                NotificationMessageFormatter.buildOrderCancelledSubject(orderId),
                NotificationMessageFormatter.buildOrderCancelledBody(orderId),
                orderId);
    }

    private void handleOutOfStock(JsonNode event) {
        long orderId = event.path("order_id").asLong();
        sendEmail(event.path("user_email").asText(""),
                NotificationMessageFormatter.buildOutOfStockSubject(),
                NotificationMessageFormatter.buildOutOfStockBody(event.path("message").asText("")),
                orderId);
    }

    private void sendEmail(String to, String subject, String body, long orderId) {
        if (to == null || to.isBlank()) {
            logger.warn("No recipient email for order {} event; skipping email", orderId);
            return;
        }
        try {
            emailSender.send(to, subject, body);
            logger.info("Email sent for order {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
        }
    }

    private void sendSms(String to, String body, long orderId) {
        if (to == null || to.isBlank()) {
            logger.warn("No recipient phone for order {} event; skipping SMS", orderId);
            return;
        }
        try {
            smsSender.send(to, body);
            logger.info("SMS sent for order {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to send SMS notification: {}", e.getMessage(), e);
        }
    }
}
