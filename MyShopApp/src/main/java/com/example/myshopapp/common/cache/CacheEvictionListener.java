package com.example.myshopapp.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 接收其它实例的缓存失效通知，只清理本地一级缓存，不再二次广播。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionListener implements MessageListener {

    private final TwoLevelCacheManager cacheManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String cacheName = new String(message.getBody(), StandardCharsets.UTF_8);
        cacheManager.invalidateLocal(cacheName);
        log.debug("已清理本地缓存：{}", cacheName);
    }
}
