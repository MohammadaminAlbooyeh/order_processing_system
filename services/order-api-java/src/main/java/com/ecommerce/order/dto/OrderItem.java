package com.ecommerce.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;

public class OrderItem {
    @JsonProperty("product_id")
    @Positive
    private int productId;

    @JsonProperty("quantity")
    @Positive
    private int quantity;

    public OrderItem() {}

    public OrderItem(int productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
