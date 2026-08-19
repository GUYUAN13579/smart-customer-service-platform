package com.example.smartcustomerservice.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpireSeconds;
    private final long refreshTokenExpireSeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expire-seconds}") long accessTokenExpireSeconds,
            @Value("${app.jwt.refresh-token-expire-seconds}") long refreshTokenExpireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
        this.refreshTokenExpireSeconds = refreshTokenExpireSeconds;
    }

    public String createAccessToken(Long userId, String username, List<String> roleCodes) {
        return createToken(userId, username, roleCodes, "access", accessTokenExpireSeconds);
    }

    public String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, List.of(), "refresh", refreshTokenExpireSeconds);
    }

    private String createToken(Long userId, String username, List<String> roleCodes, String tokenType, long expireSeconds) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleCodes", roleCodes)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoleCodes(String token) {
        Object roleCodes = parseToken(token).get("roleCodes");
        if (roleCodes instanceof List<?>) {
            return ((List<?>) roleCodes).stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    public String getTokenType(String token) {
        return parseToken(token).get("tokenType", String.class);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public LocalDateTime getExpireAt(String token) {
        Date expiration = parseToken(token).getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    public long getAccessTokenExpireSeconds() {
        return accessTokenExpireSeconds;
    }

    public long getRefreshTokenExpireSeconds() {
        return refreshTokenExpireSeconds;
    }
}