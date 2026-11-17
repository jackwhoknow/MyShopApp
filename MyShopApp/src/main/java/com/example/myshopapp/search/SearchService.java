package com.example.myshopapp.search;

import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 搜索增强：热搜榜、联想词与个人搜索历史。
 * 全部基于 Redis 与 MySQL，不引入额外搜索引擎，本地即可运行。
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final String HOT_KEY = "myshop:search:hot";
    private static final String HISTORY_PREFIX = "myshop:search:history:";
    private static final int HOT_KEEP = 50;
    private static final int HISTORY_KEEP = 10;

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    /** 记录搜索词：累加热搜分数，并写入当前用户的历史（未登录只记热搜） */
    public void record(String keyword, Long userId) {
        String normalized = normalize(keyword);
        if (normalized == null) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(HOT_KEY, normalized, 1);
        // 只保留分数最高的 50 个词，避免键无限增长
        redisTemplate.opsForZSet().removeRange(HOT_KEY, 0, -HOT_KEEP - 1);

        if (userId != null) {
            String historyKey = HISTORY_PREFIX + userId;
            redisTemplate.opsForList().remove(historyKey, 0, normalized);
            redisTemplate.opsForList().leftPush(historyKey, normalized);
            redisTemplate.opsForList().trim(historyKey, 0, HISTORY_KEEP - 1);
        }
    }

    /** 热搜榜：按搜索次数倒序 */
    public List<String> hot(int limit) {
        int size = Math.max(1, Math.min(limit, HOT_KEEP));
        return redisTemplate.opsForZSet().reverseRange(HOT_KEY, 0, size - 1)
                .stream().toList();
    }

    /** 联想词：按商品名匹配，销量高的优先 */
    public List<String> suggest(String keyword, int limit) {
        String normalized = normalize(keyword);
        if (normalized == null) {
            return List.of();
        }
        int size = Math.max(1, Math.min(limit, 10));
        return productRepository.suggestNames(ProductStatus.ON_SHELF, normalized,
                org.springframework.data.domain.PageRequest.of(0, size));
    }

    /** 当前用户最近搜索词，最新的在最前 */
    public List<String> history(Long userId) {
        return redisTemplate.opsForList().range(HISTORY_PREFIX + userId, 0, HISTORY_KEEP - 1)
                .stream().toList();
    }

    public void clearHistory(Long userId) {
        redisTemplate.delete(HISTORY_PREFIX + userId);
    }

    private String normalize(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
    }
}
