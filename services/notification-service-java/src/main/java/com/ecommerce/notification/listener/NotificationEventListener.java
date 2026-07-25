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
 * original Python service (lpush on the producer side, lpop here). Kept as a simple
 * poll-and-sleep loop rather than Redis pub/sub so the wire contract with
 * worker-service / order-api does not change.
 */
@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final String QUEUE_KEY = "notification_queue";

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
            if ("ORDER_PROCESSED".equals(type)) {
                sendOrderConfirmationEmail(event);
                sendSmsNotification(event);
            }
        } catch (Exception e) {
            logger.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }

    private void sendOrderConfirmationEmail(JsonNode order) {
        long orderId = order.path("order_id").asLong();
        String subject = NotificationMessageFormatter.buildOrderConfirmationSubject(orderId);
        String body = NotificationMessageFormatter.buildOrderConfirmationBody(orderId, order.path("recommendations"));

        try {
            emailSender.send(order.path("user_email").asText(""), subject, body);
            logger.info("Email sent for order {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to send confirmation email: {}", e.getMessage(), e);
        }
    }

    private void sendSmsNotification(JsonNode order) {
        long orderId = order.path("order_id").asLong();
        String smsBody = NotificationMessageFormatter.buildTrackingSmsBody(orderId);

        try {
            smsSender.send(order.path("user_phone").asText(""), smsBody);
            logger.info("SMS sent for order {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to send SMS notification: {}", e.getMessage(), e);
        }
    }
}
