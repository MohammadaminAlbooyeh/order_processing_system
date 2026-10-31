package com.ecommerce.order.repository;

import com.ecommerce.order.dto.OrderItem;
import com.ecommerce.order.model.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    public Optional<Order> findById(Long orderId) {
        List<Order> rows = jdbcTemplate.query(
                "SELECT id, user_id, status, destination, weight, items::text AS items_json, created_at, updated_at FROM orders WHERE id = ?",
                (rs, rowNum) -> {
                    Order order = new Order();
                    order.setId(rs.getLong("id"));
                    order.setUserId(rs.getLong("user_id"));
                    order.setStatus(rs.getString("status"));
                    order.setDestination(rs.getString("destination"));
                    order.setWeight(rs.getDouble("weight"));
                    order.setItems(rs.getString("items_json"));
                    order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return order;
                },
                orderId
        );
        return rows.stream().findFirst();
    }

    public List<Order> findByUserId(Long userId, String status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT id, user_id, status, destination, weight, items::text AS items_json, created_at, updated_at FROM orders WHERE user_id = ?");
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        List<Order> rows = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> {
                    Order order = new Order();
                    order.setId(rs.getLong("id"));
                    order.setUserId(rs.getLong("user_id"));
                    order.setStatus(rs.getString("status"));
                    order.setDestination(rs.getString("destination"));
                    order.setWeight(rs.getDouble("weight"));
                    order.setItems(rs.getString("items_json"));
                    order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return order;
                },
                status != null && !status.isBlank() ? new Object[]{userId, status, limit, offset} : new Object[]{userId, limit, offset}
        );
        return rows;
    }

    public int countByUserId(Long userId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM orders WHERE user_id = ?");
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
        }
        if (status != null && !status.isBlank()) {
            return jdbcTemplate.queryForObject(sql.toString(), Integer.class, userId, status);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, userId);
    }

    public void update(Order order) {
        jdbcTemplate.update(
                "UPDATE orders SET status = ?, destination = ?, weight = ?, items = ?, updated_at = ? WHERE id = ?",
                order.getStatus(), order.getDestination(), order.getWeight(), order.getItems(), order.getUpdatedAt(), order.getId()
        );
    }

    public void delete(Long orderId) {
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
    }

    public long createOrder(Order order) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO orders (user_id, status, destination, weight, items, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                order.getUserId(),
                order.getStatus(),
                order.getDestination(),
                order.getWeight(),
                order.getItems(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
        return id;
    }

    public List<OrderItem> parseItems(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<OrderItem>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
