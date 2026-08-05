package com.ecommerce.order.dto;

public class OrderCreateRequest {
    private String destination;
    private Double weight;
    private java.util.List<OrderItem> items;

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public java.util.List<OrderItem> getItems() { return items; }
    public void setItems(java.util.List<OrderItem> items) { this.items = items; }
}
