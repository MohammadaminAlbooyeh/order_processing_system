package com.ecommerce.order.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OrderUpdateRequest {

    @Pattern(regexp = "pending|processing|shipped|delivered|cancelled",
            message = "status must be one of: pending, processing, shipped, delivered, cancelled")
    private String status;

    @Size(max = 500)
    private String destination;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}
