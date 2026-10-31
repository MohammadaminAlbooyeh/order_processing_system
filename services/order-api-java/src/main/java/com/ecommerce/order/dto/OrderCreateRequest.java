package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class OrderCreateRequest {

    @NotBlank
    @Size(max = 500)
    private String destination;

    @NotNull
    @Positive
    private Double weight;

    @NotEmpty
    @Valid
    private java.util.List<OrderItem> items;

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public java.util.List<OrderItem> getItems() { return items; }
    public void setItems(java.util.List<OrderItem> items) { this.items = items; }
}
