package com.example.myshopapp.auth;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录频率限制：按客户端 IP 统计窗口期内的登录请求次数，防止暴力破解与撞库。
 * 计数存放在 Redis，多实例共享同一份配额；首次计数时通过 Lua 脚本原子设置过期时间。
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String KEY_PREFIX = "myshop:login:ip:";

    /** INCR 与 EXPIRE 放在同一脚本里执行，避免计数键不过期或并发设置过期时间 */
    private static final RedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 记录一次登录请求，超出窗口配额时拒绝。
     */
    public void checkAndRecord(String ip, int limit, int windowSeconds) {
        if (limit <= 0) {
            return;
        }
        int seconds = Math.max(1, windowSeconds);
        Long count = redisTemplate.execute(INCR_WITH_EXPIRE,
                List.of(KEY_PREFIX + (ip == null ? "unknown" : ip)), String.valueOf(seconds));
        if (count != null && count > limit) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录尝试过于频繁，请稍后再试");
        }
    }
}
