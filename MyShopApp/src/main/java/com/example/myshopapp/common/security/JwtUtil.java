package com.example.myshopapp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 签发与解析。载荷里放 userId 和角色，避免每次请求都查库。
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.username())
                .claim("uid", user.id())
                .claim("roles", user.roles())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public LoginUser parseToken(String token) {
        return parse(token).loginUser();
    }

    /**
     * 解析 token 并返回签发/过期时间，供吊销校验使用。
     */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number uid = claims.get("uid", Number.class);
        List<?> rawRoles = claims.get("roles", List.class);
        Set<String> roles = rawRoles == null ? Set.of()
                : rawRoles.stream().map(String::valueOf).collect(Collectors.toSet());

        LoginUser loginUser = new LoginUser(uid == null ? null : uid.longValue(), claims.getSubject(), roles);
        long issuedAt = claims.getIssuedAt() == null ? 0L : claims.getIssuedAt().getTime();
        long expiresAt = claims.getExpiration() == null ? 0L : claims.getExpiration().getTime();
        return new ParsedToken(loginUser, issuedAt, expiresAt);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public record ParsedToken(LoginUser loginUser, long issuedAt, long expiresAt) {
    }
}
