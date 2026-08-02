package com.asad.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes,
            @Value("${app.security.jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be set to a random string of at least 32 characters. " +
                            "Set the JWT_SECRET environment variable.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMs = accessTokenTtlMinutes * 60_000;
        this.refreshTokenTtlMs = refreshTokenTtlDays * 24 * 60 * 60_000;
    }

    public String generateAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenTtlMs))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenTtlMs))
                .signWith(signingKey)
                .compact();
    }

    public long getRefreshTokenTtlMs() {
        return refreshTokenTtlMs;
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("uid", Long.class);
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    public boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = extractAllClaims(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            boolean correctType = expectedType.equals(claims.get("type", String.class));
            return notExpired && correctType;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** Hash a refresh token for safe storage/comparison (never persist raw tokens). */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }
}
