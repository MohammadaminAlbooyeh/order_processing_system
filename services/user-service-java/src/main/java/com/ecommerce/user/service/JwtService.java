package com.ecommerce.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final String SECRET_KEY = System.getenv("SECRET_KEY");
    private static final String ALGORITHM = "HS256";
    private static final long EXPIRATION_MINUTES = 60;

    public String createToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));
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
            SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));
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
