package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderListResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.OrderUpdateRequest;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderCreateRequest body) {
        Long userId = getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Invalid token"));
        }

        try {
            OrderResponse response = orderService.createOrder(userId, body.getDestination(), body.getWeight(), body.getItems());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", "Internal server error"));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        Long userId = getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Invalid token"));
        }

        Optional<OrderResponse> response = orderService.getOrder(orderId, userId);
        if (response.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Order not found"));
        }
        return ResponseEntity.ok(response.get());
    }

    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        Long userId = getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Invalid token"));
        }

        long total = orderService.countOrders(userId, status);
        List<OrderResponse> orders = orderService.listOrders(userId, status, limit, offset);
        return ResponseEntity.ok(new OrderListResponse(orders, total, limit, offset));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable Long orderId, @Valid @RequestBody OrderUpdateRequest body) {
        Long userId = getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Invalid token"));
        }

        try {
            Optional<OrderResponse> response = orderService.updateOrder(orderId, userId, body.getStatus(), body.getDestination());
            if (response.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Order not found"));
            }
            return ResponseEntity.ok(response.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", "Internal server error"));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        Long userId = getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Invalid token"));
        }

        try {
            boolean cancelled = orderService.cancelOrder(orderId, userId);
            if (!cancelled) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Order not found"));
            }
            return ResponseEntity.ok(Map.of("detail", "Order cancelled successfully", "order_id", orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", "Internal server error"));
        }
    }

    private Long getUserId() {
        Object userId = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                .getAttribute("userId", org.springframework.web.context.request.RequestContextHolder.SCOPE_REQUEST);
        if (userId instanceof Long l) {
            return l;
        }
        return null;
    }
}
