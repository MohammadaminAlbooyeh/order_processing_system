package com.ecommerce.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final long EXPIRATION_MINUTES = 60;

    /** Must match com.ecommerce.order.util.JwtUtil#DEFAULT_DEV_SECRET. */
    static final String DEFAULT_DEV_SECRET = "insecure-dev-secret-change-me-in-production";

    private final SecretKey key;

    public JwtService(@Value("${SECRET_KEY:}") String secret) {
        this.key = deriveKey(secret);
    }

    /**
     * Derives a 256-bit HMAC key by SHA-256 of the secret string. This accepts any
     * secret (any length, any encoding) so a misconfigured value can never crash
     * token creation; order-api derives its verification key identically.
     */
    static SecretKey deriveKey(String secret) {
        String effective = (secret == null || secret.isBlank()) ? DEFAULT_DEV_SECRET : secret;
        if (effective.equals(DEFAULT_DEV_SECRET) && "prod".equalsIgnoreCase(System.getenv("APP_ENV"))) {
            throw new IllegalStateException(
                    "SECRET_KEY must be set to a non-default value when APP_ENV=prod");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(effective.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String createToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String sub = claims.getSubject();
            if (sub != null) {
                return Long.parseLong(sub);
            }
        } catch (Exception e) {
            logger.debug("Invalid token: {}", e.getMessage());
        }
        return null;
    }
}
