package com.ecommerce.order.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Read-only lookup of a user's contact details from the shared {@code users}
 * table, used to enrich order events with the recipient address before they are
 * handed to notification-service (which has no database of its own).
 */
@Repository
public class UserLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserLookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Returns {@code {email, phone}} for the user, or an empty map if not found. */
    public Map<String, String> findContact(Long userId) {
        return jdbcTemplate.query(
                "SELECT email, phone FROM users WHERE id = ?",
                rs -> rs.next()
                        ? Map.of(
                                "email", rs.getString("email") != null ? rs.getString("email") : "",
                                "phone", rs.getString("phone") != null ? rs.getString("phone") : "")
                        : Map.of(),
                userId
        );
    }
}
