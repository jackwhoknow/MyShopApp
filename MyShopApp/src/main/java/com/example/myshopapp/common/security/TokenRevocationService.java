package com.example.myshopapp.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token 吊销记录（Redis 存储，多实例共享，重启不丢失）：
 * 1. 退出登录时把当前 token 加入黑名单，过期时间与 token 自身过期时间对齐；
 * 2. 修改密码/禁用账号时按用户记录吊销时间点，该时间点之前签发的 token 全部失效。
 */
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private static final String TOKEN_PREFIX = "myshop:auth:revoked:token:";
    private static final String USER_PREFIX = "myshop:auth:revoked:user:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    /** 退出登录：token 在自身过期前一直处于黑名单 */
    public void revokeToken(String token, long expiresAtEpochMillis) {
        if (token == null) {
            return;
        }
        long ttlMillis = expiresAtEpochMillis - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(TOKEN_PREFIX + token, "1", Duration.ofMillis(ttlMillis));
        }
    }

    /** 修改密码/禁用账号：吊销该用户已签发的 token */
    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        // 已签发 token 最长存活一个有效期，吊销标记保留同样时长即可全部覆盖
        redisTemplate.opsForValue().set(USER_PREFIX + userId,
                String.valueOf(System.currentTimeMillis()),
                Duration.ofMillis(jwtUtil.getExpirationMs()));
    }

    public boolean isRevoked(String token, Long userId, long issuedAtEpochMillis) {
        if (token != null && Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token))) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        String revokedBefore = redisTemplate.opsForValue().get(USER_PREFIX + userId);
        if (revokedBefore == null) {
            return false;
        }
        try {
            return issuedAtEpochMillis <= Long.parseLong(revokedBefore);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
