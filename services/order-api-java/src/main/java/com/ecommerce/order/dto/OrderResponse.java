package com.ecommerce.order.dto;

public class OrderResponse {
    private Long id;
    private Long userId;
    private String status;
    private String destination;
    private Double weight;
    private java.util.List<java.util.Map<String, Object>> items;
    private String createdAt;
    private String updatedAt;

    public OrderResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public java.util.List<java.util.Map<String, Object>> getItems() { return items; }
    public void setItems(java.util.List<java.util.Map<String, Object>> items) { this.items = items; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
