package com.example.myshopapp.common.config;

import com.example.myshopapp.common.cache.CacheEvictionListener;
import com.example.myshopapp.common.cache.CacheEvictionPublisher;
import com.example.myshopapp.common.cache.TwoLevelCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存配置：Caffeine 本地一级缓存 + Redis 二级缓存，统一 key 前缀与 JSON 序列化。
 * <p>
 * 本地缓存有效期短于 Redis，写操作失效时两级同时清理，并通过 pub/sub 通知其它实例清本地缓存。
 * 缓存只承载可重算的读数据，Redis 故障时按缓存未命中处理，不影响下单等写链路。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public TwoLevelCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     CacheEvictionPublisher evictionPublisher,
                                     @Value("${myshop.cache.local-ttl-seconds:30}") long localTtlSeconds,
                                     @Value("${myshop.cache.local-max-size:10000}") long localMaxSize) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .computePrefixWith(name -> "myshop:cache:" + name + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
        CacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(localMaxSize)
                .expireAfterWrite(Duration.ofSeconds(localTtlSeconds));
        return new TwoLevelCacheManager(redisCacheManager, caffeine, evictionPublisher);
    }

    /** 订阅缓存失效频道，收到其它实例的失效通知后清理本机一级缓存 */
    @Bean
    public RedisMessageListenerContainer cacheEvictionListenerContainer(
            RedisConnectionFactory connectionFactory, CacheEvictionListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic(CacheEvictionPublisher.CHANNEL));
        return container;
    }
}
