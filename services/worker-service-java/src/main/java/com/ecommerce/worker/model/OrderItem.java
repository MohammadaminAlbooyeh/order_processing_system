package com.ecommerce.worker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderItem(
        @JsonProperty("product_id") int productId,
        @JsonProperty("quantity") int quantity
) {
}
