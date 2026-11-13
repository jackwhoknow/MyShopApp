package com.example.myshopapp.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 二级缓存失效广播：本机清理 Redis 后，通过 pub/sub 通知其它实例丢弃本地一级缓存，
 * 避免多实例部署时某台机器继续返回旧数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionPublisher {

    public static final String CHANNEL = "myshop:cache:evict";

    private final StringRedisTemplate redisTemplate;

    /**
     * 广播某个缓存已失效。本地缓存按名整体清空：缓存条目少、写入频率低，
     * 比按 key 反序列化更稳妥（避免 Long/String 键类型不一致导致失效遗漏）。
     */
    public void publish(String cacheName) {
        try {
            redisTemplate.convertAndSend(CHANNEL, cacheName);
        } catch (RuntimeException e) {
            // 广播失败只影响其它实例的本地缓存新鲜度，等待本地 TTL 自然过期即可
            log.warn("缓存失效广播失败：cache={}，{}", cacheName, e.getMessage());
        }
    }
}
