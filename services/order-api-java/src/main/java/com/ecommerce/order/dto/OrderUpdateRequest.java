package com.ecommerce.order.dto;

public class OrderUpdateRequest {
    private String status;
    private String destination;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}
