package com.asad.expensetracker.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "unit-test-secret-that-is-at-least-32-characters-long", 15, 7);

    @Test
    void generatesAndValidatesAccessToken() {
        String token = jwtService.generateAccessToken(1L, "user@example.com", "USER");

        assertThat(jwtService.isTokenValid(token, "access")).isTrue();
        assertThat(jwtService.isTokenValid(token, "refresh")).isFalse();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        String token = jwtService.generateRefreshToken(42L, "someone@example.com");

        assertThat(jwtService.isTokenValid(token, "refresh")).isTrue();
        assertThat(jwtService.isTokenValid(token, "access")).isFalse();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtService.isTokenValid("not-a-real-token", "access")).isFalse();
    }

    @Test
    void hashTokenIsDeterministicAndDistinct() {
        String hashA = jwtService.hashToken("token-a");
        String hashB = jwtService.hashToken("token-a");
        String hashC = jwtService.hashToken("token-b");

        assertThat(hashA).isEqualTo(hashB);
        assertThat(hashA).isNotEqualTo(hashC);
    }

    @Test
    void rejectsSecretShorterThan32Chars() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short", 15, 7));
    }
}
