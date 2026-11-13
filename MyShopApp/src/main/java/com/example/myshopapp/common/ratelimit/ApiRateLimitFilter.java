package com.example.myshopapp.common.ratelimit;

import com.example.myshopapp.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流：基于 Redis 令牌桶按客户端 IP 限制请求速率，多实例部署时共享配额。
 * <p>
 * 登录、注册、下单等敏感接口使用更严格的配额；Redis 不可用时直接放行（fail-open），
 * 限流是保护手段而不是可用性前提。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    /**
     * 令牌桶：tokens 为当前令牌数，ts 为上次补充时间。
     * KEYS[1] 桶键；ARGV 依次为每秒补充速率、桶容量、当前毫秒时间戳、键过期毫秒。
     */
    private static final RedisScript<Long> TOKEN_BUCKET_SCRIPT = RedisScript.of("""
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
            local ts = tonumber(redis.call('HGET', KEYS[1], 'ts'))
            local rate = tonumber(ARGV[1])
            local burst = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            if tokens == nil then
                tokens = burst
                ts = now
            end
            local delta = now - ts
            if delta > 0 then
                tokens = math.min(burst, tokens + (delta * rate) / 1000)
            end
            local allowed = 0
            if tokens >= 1 then
                tokens = tokens - 1
                allowed = 1
            end
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            return allowed
            """, Long.class);

    private static final String KEY_PREFIX = "myshop:ratelimit:";
    private static final long BUCKET_TTL_MS = 60_000L;

    /** 敏感路径使用独立配额，按最长前缀匹配 */
    private static final List<PathRule> PATH_RULES = List.of(
            new PathRule("/api/auth/login", Scope.AUTH),
            new PathRule("/api/auth/register", Scope.AUTH),
            new PathRule("/api/auth/captcha", Scope.AUTH),
            new PathRule("/api/orders/seckill", Scope.ORDER),
            new PathRule("/api/orders", Scope.ORDER));

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${myshop.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${myshop.ratelimit.default-rate:50}")
    private int defaultRate;

    @Value("${myshop.ratelimit.default-burst:100}")
    private int defaultBurst;

    @Value("${myshop.ratelimit.sensitive-rate:5}")
    private int sensitiveRate;

    @Value("${myshop.ratelimit.sensitive-burst:10}")
    private int sensitiveBurst;

    @Value("${myshop.ratelimit.order-rate:10}")
    private int orderRate;

    @Value("${myshop.ratelimit.order-burst:20}")
    private int orderBurst;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || HttpMethod.OPTIONS.matches(request.getMethod()) || !shouldLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Scope scope = resolveScope(request.getRequestURI());
        String clientIp = resolveClientIp(request);
        String key = KEY_PREFIX + scope.name().toLowerCase() + ":" + clientIp;
        Limit limit = resolveLimit(scope);

        try {
            Long allowed = redisTemplate.execute(TOKEN_BUCKET_SCRIPT, List.of(key),
                    String.valueOf(limit.ratePerSecond()),
                    String.valueOf(limit.burst()),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(BUCKET_TTL_MS));
            if (allowed != null && allowed == 0L) {
                writeTooManyRequests(response);
                return;
            }
        } catch (RuntimeException e) {
            // 限流组件故障不能阻断业务请求
            log.warn("接口限流执行失败，放行请求：{}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldLimit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 支付平台回调由签名验签保护，不对第三方服务器做限流
        return uri.startsWith("/api/") && !uri.contains("/notify");
    }

    private Scope resolveScope(String uri) {
        for (PathRule rule : PATH_RULES) {
            if (uri.startsWith(rule.pathPrefix())) {
                return rule.scope();
            }
        }
        return Scope.DEFAULT;
    }

    private Limit resolveLimit(Scope scope) {
        return switch (scope) {
            case AUTH -> new Limit(sensitiveRate, sensitiveBurst);
            case ORDER -> new Limit(orderRate, orderBurst);
            default -> new Limit(defaultRate, defaultBurst);
        };
    }

    /**
     * 真实客户端 IP：server.forward-headers-strategy=framework 会把代理头还原到 remoteAddr，
     * 这里再兜底读取 X-Forwarded-For 的首个地址。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(TimeUnit.SECONDS.toSeconds(1)));
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(429, "请求过于频繁，请稍后再试"));
    }

    private enum Scope {
        DEFAULT, AUTH, ORDER
    }

    private record PathRule(String pathPrefix, Scope scope) {
    }

    private record Limit(int ratePerSecond, int burst) {
    }
}
