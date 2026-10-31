package com.ecommerce.user.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret");

    @Test
    void createToken_thenExtractUserId_roundTrips() {
        String token = jwtService.createToken(42L);

        Long userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void extractUserId_malformedToken_returnsNull() {
        Long userId = jwtService.extractUserId("not-a-valid-token");

        assertThat(userId).isNull();
    }

    @Test
    void extractUserId_tamperedToken_returnsNull() {
        String token = jwtService.createToken(7L);
        String[] parts = token.split("\\.");
        String tamperedSignature = new StringBuilder(parts[2]).reverse().toString();
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        Long userId = jwtService.extractUserId(tampered);

        assertThat(userId).isNull();
    }
}
