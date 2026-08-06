package com.ecommerce.notification.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageFormatterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void formatRecommendations_nullNode_returnsFallbackMessage() {
        assertThat(NotificationMessageFormatter.formatRecommendations(null))
                .isEqualTo("No recommendations yet.");
    }

    @Test
    void formatRecommendations_emptyArray_returnsFallbackMessage() {
        ArrayNode empty = objectMapper.createArrayNode();

        assertThat(NotificationMessageFormatter.formatRecommendations(empty))
                .isEqualTo("No recommendations yet.");
    }

    @Test
    void formatRecommendations_nonArrayNode_returnsFallbackMessage() {
        JsonNode textNode = objectMapper.getNodeFactory().textNode("not-an-array");

        assertThat(NotificationMessageFormatter.formatRecommendations(textNode))
                .isEqualTo("No recommendations yet.");
    }

    @Test
    void formatRecommendations_withItems_joinsAsBulletList() {
        ArrayNode items = objectMapper.createArrayNode();
        items.add("Product A");
        items.add("Product B");

        assertThat(NotificationMessageFormatter.formatRecommendations(items))
                .isEqualTo("- Product A\n- Product B");
    }

    @Test
    void buildOrderConfirmationSubject_includesOrderId() {
        assertThat(NotificationMessageFormatter.buildOrderConfirmationSubject(123L))
                .isEqualTo("Order #123 Confirmed");
    }

    @Test
    void buildOrderConfirmationBody_includesOrderIdAndRecommendations() {
        ArrayNode items = objectMapper.createArrayNode();
        items.add("Product A");

        String body = NotificationMessageFormatter.buildOrderConfirmationBody(123L, items);

        assertThat(body).contains("Order ID: 123");
        assertThat(body).contains("- Product A");
    }

    @Test
    void buildTrackingSmsBody_includesOrderIdAndTrackingLink() {
        assertThat(NotificationMessageFormatter.buildTrackingSmsBody(123L))
                .isEqualTo("Order 123 confirmed! Track at: ecommerce.com/track/123");
    }
}
