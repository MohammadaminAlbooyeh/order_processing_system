package com.ecommerce.order.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies JWTs issued by user-service. The signing key is derived by SHA-256 of
 * the shared {@code SECRET_KEY} so any secret string (regardless of length or
 * encoding) yields a valid 256-bit HMAC key. user-service derives its key the
 * exact same way, so tokens round-trip between the two services.
 */
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    /** Must match com.ecommerce.user.service.JwtService#DEFAULT_DEV_SECRET. */
    static final String DEFAULT_DEV_SECRET = "insecure-dev-secret-change-me-in-production";

    private final SecretKey key;

    public JwtUtil() {
        this(System.getenv("SECRET_KEY"));
    }

    public JwtUtil(String secret) {
        this.key = deriveKey(secret);
    }

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
