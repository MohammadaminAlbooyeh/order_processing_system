package com.ecommerce.order.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Base64;

public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String SECRET_KEY = System.getenv("SECRET_KEY");
    private static final String ALGORITHM = "HS256";

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
