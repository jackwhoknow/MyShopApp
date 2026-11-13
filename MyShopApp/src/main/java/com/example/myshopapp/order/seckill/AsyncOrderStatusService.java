package com.example.myshopapp.order.seckill;

import com.example.myshopapp.order.dto.AsyncOrderVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 排队下单状态存储：提交时写 PENDING，消费完成后改写 SUCCESS/FAILED。
 * 状态放在 Redis 中并带过期时间，多实例部署时前端轮询可命中任意节点。
 */
@Service
@RequiredArgsConstructor
public class AsyncOrderStatusService {

    private static final String KEY_PREFIX = "myshop:order:req:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${myshop.order.async.result-ttl-minutes:60}")
    private long ttlMinutes;

    /**
     * 抢占请求编号：只有第一次提交能写入成功，重复提交直接复用已有状态。
     */
    public boolean markPending(Long userId, String requestId) {
        AsyncOrderState state = new AsyncOrderState(
                AsyncOrderVO.PENDING, null, null, "已进入下单队列，正在为您锁定库存", userId);
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(key(requestId), write(state), ttl()));
    }

    public void markSuccess(String requestId, Long orderId, BigDecimal totalAmount) {
        AsyncOrderState current = find(requestId);
        if (current == null) {
            return;
        }
        save(requestId, new AsyncOrderState(
                AsyncOrderVO.SUCCESS, orderId, totalAmount, "下单成功", current.userId()));
    }

    public void markFailed(String requestId, String message) {
        AsyncOrderState current = find(requestId);
        if (current == null) {
            return;
        }
        save(requestId, new AsyncOrderState(
                AsyncOrderVO.FAILED, null, null, message, current.userId()));
    }

    public AsyncOrderState find(String requestId) {
        String json = redisTemplate.opsForValue().get(key(requestId));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AsyncOrderState.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** 查询排队结果：请求编号不存在或不属于当前用户时返回 null */
    public AsyncOrderVO view(String requestId, Long userId) {
        AsyncOrderState state = find(requestId);
        if (state == null || !userId.equals(state.userId())) {
            return null;
        }
        return new AsyncOrderVO(requestId, state.status(), state.orderId(),
                state.totalAmount(), state.message());
    }

    private void save(String requestId, AsyncOrderState state) {
        redisTemplate.opsForValue().set(key(requestId), write(state), ttl());
    }

    private String write(AsyncOrderState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("下单状态序列化失败", e);
        }
    }

    private Duration ttl() {
        return Duration.ofMinutes(ttlMinutes);
    }

    private String key(String requestId) {
        return KEY_PREFIX + requestId;
    }
}
