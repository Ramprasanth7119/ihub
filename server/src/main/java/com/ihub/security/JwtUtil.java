package com.ihub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final String TYPE_ACCESS = "access";

    /** HS256 requires at least 256 bits of key material. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${spring.security.jwt.secret}") String secret,
            @Value("${spring.security.jwt.expiration}") long accessExpirationMs,
            @Value("${spring.security.jwt.refresh-expiration:604800000}") long refreshExpirationMs) {

        byte[] keyBytes = secret != null ? secret.getBytes(StandardCharsets.UTF_8) : new byte[0];
        // Fail fast at startup rather than issuing tokens signed with a weak key.
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " characters; got " + keyBytes.length);
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(String email, String role) {
        return buildToken(email, role, accessExpirationMs, TYPE_ACCESS);
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "." + UUID.randomUUID();
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    /**
     * True when the token was minted by {@link #generateAccessToken}. Guards against
     * a token issued for another purpose being replayed as an API credential.
     */
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(getClaims(token).get("type"));
    }

    private String buildToken(String email, String role, long expirationMs, String type) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("type", type)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
