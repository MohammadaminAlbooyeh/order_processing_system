package com.ecommerce.notification.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageFormatterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void formatRecommendations_returnsPlaceholder_whenEmpty() throws Exception {
        JsonNode empty = objectMapper.readTree("[]");
        assertThat(NotificationMessageFormatter.formatRecommendations(empty)).isEqualTo("No recommendations yet.");
    }

    @Test
    void formatRecommendations_returnsPlaceholder_whenMissing() {
        assertThat(NotificationMessageFormatter.formatRecommendations(null)).isEqualTo("No recommendations yet.");
    }

    @Test
    void formatRecommendations_listsEachItem() throws Exception {
        JsonNode recommendations = objectMapper.readTree("[11, 12, 14]");
        assertThat(NotificationMessageFormatter.formatRecommendations(recommendations))
                .isEqualTo("- 11\n- 12\n- 14");
    }

    @Test
    void buildOrderConfirmationSubject_includesOrderId() {
        assertThat(NotificationMessageFormatter.buildOrderConfirmationSubject(42L))
                .isEqualTo("Order #42 Confirmed");
    }

    @Test
    void buildTrackingSmsBody_includesTrackingLink() {
        assertThat(NotificationMessageFormatter.buildTrackingSmsBody(42L))
                .contains("ecommerce.com/track/42")
                .startsWith("Order 42 confirmed!");
    }
}
