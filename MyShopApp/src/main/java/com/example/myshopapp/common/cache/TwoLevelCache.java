package com.example.myshopapp.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;

/**
 * 两级缓存：一级为进程内 Caffeine，二级为 Redis。
 * <p>
 * 读取先查本地，未命中再查 Redis 并回填本地；写入与失效同时作用于两级。
 * Redis 异常按未命中处理，保证缓存故障不阻断业务读请求；本地缓存仍可继续服务。
 */
@Slf4j
public class TwoLevelCache implements Cache {

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> local;
    private final Cache remote;
    private final CacheEvictionPublisher publisher;

    public TwoLevelCache(String name,
                         com.github.benmanes.caffeine.cache.Cache<Object, Object> local,
                         Cache remote,
                         CacheEvictionPublisher publisher) {
        this.name = name;
        this.local = local;
        this.remote = remote;
        this.publisher = publisher;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return local;
    }

    @Override
    public ValueWrapper get(Object key) {
        Object localValue = local.getIfPresent(key);
        if (localValue != null) {
            return new SimpleValueWrapper(localValue);
        }

        ValueWrapper remoteValue;
        try {
            remoteValue = remote.get(key);
        } catch (RuntimeException e) {
            log.debug("读取 Redis 缓存失败，按未命中处理：cache={}，key={}", name, key, e);
            return null;
        }
        if (remoteValue == null) {
            return null;
        }
        Object value = remoteValue.get();
        if (value != null) {
            local.put(key, value);
        }
        return remoteValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException(
                    "缓存值与期望类型不符 [" + type.getName() + "]：" + value);
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        T value;
        try {
            value = valueLoader.call();
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
        put(key, value);
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            return;
        }
        local.put(key, value);
        try {
            remote.put(key, value);
        } catch (RuntimeException e) {
            log.warn("写入 Redis 缓存失败：cache={}，key={}，{}", name, key, e.getMessage());
        }
    }

    @Override
    public void evict(Object key) {
        local.invalidate(key);
        try {
            remote.evict(key);
        } catch (RuntimeException e) {
            log.warn("删除 Redis 缓存失败：cache={}，key={}，{}", name, key, e.getMessage());
        }
        publisher.publish(name);
    }

    @Override
    public void clear() {
        local.invalidateAll();
        try {
            remote.clear();
        } catch (RuntimeException e) {
            log.warn("清空 Redis 缓存失败：cache={}，{}", name, e.getMessage());
        }
        publisher.publish(name);
    }

    /** 仅清理本地缓存，供失效广播使用 */
    void invalidateLocal() {
        local.invalidateAll();
    }
}
