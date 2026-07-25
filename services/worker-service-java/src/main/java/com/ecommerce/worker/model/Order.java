package com.ecommerce.worker.model;

import java.util.List;

public record Order(
        long id,
        long userId,
        String status,
        String destination,
        double weight,
        List<OrderItem> items
) {
}
