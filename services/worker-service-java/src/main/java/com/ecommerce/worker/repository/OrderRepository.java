package com.ecommerce.worker.repository;

import com.ecommerce.worker.model.Order;
import com.ecommerce.worker.model.OrderItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Order> findById(long orderId) {
        List<Order> rows = jdbcTemplate.query(
                "SELECT id, user_id, status, destination, weight, items::text AS items_json "
                        + "FROM orders WHERE id = ?",
                (rs, rowNum) -> new Order(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("status"),
                        rs.getString("destination"),
                        rs.getDouble("weight"),
                        parseItems(rs.getString("items_json"))
                ),
                orderId
        );
        return rows.stream().findFirst();
    }

    public void updateStatus(long orderId, String status) {
        jdbcTemplate.update("UPDATE orders SET status = ? WHERE id = ?", status, orderId);
    }

    /** Flattened items from the user's most recent orders, newest first (mirrors get_user_purchase_history). */
    public List<OrderItem> findRecentItemsByUser(long userId, int limit) {
        List<List<OrderItem>> perOrderItems = jdbcTemplate.query(
                "SELECT items::text AS items_json FROM orders WHERE user_id = ? "
                        + "ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> parseItems(rs.getString("items_json")),
                userId, limit
        );
        List<OrderItem> history = new ArrayList<>();
        perOrderItems.forEach(history::addAll);
        return history;
    }

    private List<OrderItem> parseItems(String itemsJson) throws SQLException {
        if (itemsJson == null || itemsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<OrderItem>>() {
            });
        } catch (Exception e) {
            throw new SQLException("Failed to parse order items JSON: " + itemsJson, e);
        }
    }
}
