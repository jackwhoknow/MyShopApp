package com.example.myshopapp.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

/**
 * Refresh Token 管理：只保存 token 的 SHA-256 摘要，数据库/Redis 泄露也无法直接冒用。
 * <p>
 * 每次刷新都会消费旧的 refresh token 并签发新的（轮换），被窃取的旧 token 只能使用一次；
 * 退出登录或修改密码时按用户吊销全部 refresh token。
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String TOKEN_KEY_PREFIX = "myshop:auth:refresh:";
    private static final String USER_KEY_PREFIX = "myshop:auth:refresh:user:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    /** 签发 refresh token，返回明文给客户端，服务端只留摘要 */
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(token);

        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + hash,
                String.valueOf(userId), Duration.ofMillis(refreshExpirationMs));
        String userKey = USER_KEY_PREFIX + userId;
        redisTemplate.opsForSet().add(userKey, hash);
        redisTemplate.expire(userKey, Duration.ofMillis(refreshExpirationMs));
        return token;
    }

    /**
     * 一次性消费 refresh token，返回所属用户；不存在或已使用过返回 null。
     */
    public Long consume(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String hash = hash(token);
        String userId = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + hash);
        if (userId == null) {
            return null;
        }
        redisTemplate.opsForSet().remove(USER_KEY_PREFIX + userId, hash);
        return Long.valueOf(userId);
    }

    /** 退出登录时吊销指定 refresh token */
    public void revoke(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        String hash = hash(token);
        String key = TOKEN_KEY_PREFIX + hash;
        String userId = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_KEY_PREFIX + userId, hash);
        }
    }

    /** 吊销某用户全部 refresh token：改密、封号等场景使用 */
    public void revokeAll(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;
        Set<String> hashes = redisTemplate.opsForSet().members(userKey);
        if (hashes != null && !hashes.isEmpty()) {
            hashes.forEach(hash -> redisTemplate.delete(TOKEN_KEY_PREFIX + hash));
        }
        redisTemplate.delete(userKey);
    }

    public long getExpirationMs() {
        return refreshExpirationMs;
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }
}
