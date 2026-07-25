package com.ecommerce.notification.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class NotificationMessageFormatter {

    private NotificationMessageFormatter() {
    }

    public static String formatRecommendations(JsonNode recommendations) {
        if (recommendations == null || !recommendations.isArray() || recommendations.isEmpty()) {
            return "No recommendations yet.";
        }
        List<String> lines = new ArrayList<>();
        recommendations.forEach(r -> lines.add("- " + r.asText()));
        return String.join("\n", lines);
    }

    public static String buildOrderConfirmationSubject(long orderId) {
        return "Order #" + orderId + " Confirmed";
    }

    public static String buildOrderConfirmationBody(long orderId, JsonNode recommendations) {
        return """
                Thank you for your order!
                Order ID: %d
                Estimated delivery: 3-5 business days

                Recommended products for you:
                %s
                """.formatted(orderId, formatRecommendations(recommendations));
    }

    public static String buildTrackingSmsBody(long orderId) {
        return "Order " + orderId + " confirmed! Track at: ecommerce.com/track/" + orderId;
    }
}
