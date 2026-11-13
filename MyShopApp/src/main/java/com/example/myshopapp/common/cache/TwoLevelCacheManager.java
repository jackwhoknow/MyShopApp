package com.example.myshopapp.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 两级缓存管理器：包装 Redis 缓存管理器，为每个缓存名创建一个本地 Caffeine 实例。
 * 本地实例按需创建，缓存名与 Redis 侧保持一致（当前为 product）。
 */
public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager remoteCacheManager;
    private final Caffeine<Object, Object> caffeine;
    private final CacheEvictionPublisher publisher;
    private final Map<String, TwoLevelCache> caches = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager remoteCacheManager,
                                Caffeine<Object, Object> caffeine,
                                CacheEvictionPublisher publisher) {
        this.remoteCacheManager = remoteCacheManager;
        this.caffeine = caffeine;
        this.publisher = publisher;
    }

    @Override
    public Cache getCache(String name) {
        Cache remote = remoteCacheManager.getCache(name);
        if (remote == null) {
            return null;
        }
        return caches.computeIfAbsent(name,
                key -> new TwoLevelCache(key, caffeine.build(), remote, publisher));
    }

    @Override
    public Collection<String> getCacheNames() {
        return remoteCacheManager.getCacheNames();
    }

    /** 收到其它实例的失效广播后，丢弃本机一级缓存 */
    public void invalidateLocal(String cacheName) {
        TwoLevelCache cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidateLocal();
        }
    }
}
