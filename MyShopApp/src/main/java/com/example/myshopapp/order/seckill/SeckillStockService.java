package com.example.myshopapp.order.seckill;

import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 秒杀库存预扣：下单前用 Lua 在 Redis 原子扣减可售库存，把并发压力挡在数据库之前。
 * <p>
 * Redis 中的库存第一次被访问时从数据库回源初始化，之后与数据库保持同步：
 * 下单成功由消费端落库，取消/售后退款/商家改库存时通过 {@link #adjust} 回补差额。
 * Redis 故障不影响取消订单等主流程，调用方按尽力而为处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillStockService {

    /** 秒杀库存在 Redis 中的键前缀 */
    private static final String KEY_PREFIX = "myshop:stock:";

    /**
     * 原子预扣：任一 SKU 库存不足则整单不扣，返回第一个不足的 key 下标（从 1 开始）。
     * KEYS 为库存键，ARGV 为对应购买数量。
     */
    private static final RedisScript<Long> DEDUCT_SCRIPT = RedisScript.of("""
            for i = 1, #KEYS do
                local current = tonumber(redis.call('GET', KEYS[i]))
                if current == nil or current < tonumber(ARGV[i]) then
                    return i
                end
            end
            for i = 1, #KEYS do
                redis.call('DECRBY', KEYS[i], tonumber(ARGV[i]))
            end
            return 0
            """, Long.class);

    /** 库存回补/调整：只对已初始化的键做增减，避免凭空造出库存 */
    private static final RedisScript<Long> ADJUST_SCRIPT = RedisScript.of("""
            for i = 1, #KEYS do
                if redis.call('EXISTS', KEYS[i]) == 1 then
                    redis.call('INCRBY', KEYS[i], tonumber(ARGV[i]))
                end
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ProductSkuRepository productSkuRepository;

    /**
     * 尝试预扣整单库存。
     *
     * @param quantities SKU id -> 购买数量
     * @return 扣减结果；失败时返回可直接展示给买家的原因
     */
    public DeductResult tryDeduct(Map<Long, Integer> quantities) {
        if (quantities.isEmpty()) {
            return DeductResult.ok();
        }

        Map<Long, ProductSku> skuById = productSkuRepository
                .findAllWithProductByIdIn(quantities.keySet())
                .stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        List<Long> skuIds = new ArrayList<>(quantities.keySet());
        for (Long skuId : skuIds) {
            if (!skuById.containsKey(skuId)) {
                return new DeductResult(false, "商品已不存在，请刷新后重试");
            }
        }

        // 首次访问时按数据库库存初始化；SETNX 保证多实例并发只有一个写入生效
        for (Long skuId : skuIds) {
            redisTemplate.opsForValue().setIfAbsent(
                    KEY_PREFIX + skuId,
                    String.valueOf(skuById.get(skuId).getStock()));
        }

        String[] args = skuIds.stream()
                .map(skuId -> String.valueOf(quantities.get(skuId)))
                .toArray(String[]::new);
        List<String> keys = skuIds.stream().map(this::key).toList();

        Long failedIndex = redisTemplate.execute(DEDUCT_SCRIPT, keys, (Object[]) args);
        if (failedIndex == null || failedIndex == 0) {
            return DeductResult.ok();
        }

        Long failedSkuId = skuIds.get(failedIndex.intValue() - 1);
        String productName = skuById.get(failedSkuId).getProduct().getName();
        return new DeductResult(false, "商品「" + productName + "」库存不足，请减少购买数量");
    }

    /** 回补整单预扣的库存（下单失败、用户取消等场景） */
    public void restore(Map<Long, Integer> quantities) {
        quantities.forEach(this::adjust);
    }

    /**
     * 事务提交后再调整 Redis 库存：库存变更随数据库事务回滚时不产生偏差。
     */
    public void adjustAfterCommit(Long skuId, int delta) {
        if (delta == 0) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    adjust(skuId, delta);
                }
            });
        } else {
            adjust(skuId, delta);
        }
    }

    /**
     * 按差额调整 Redis 库存：正数回补、负数扣减，仅作用于已初始化的 SKU。
     * 库存类操作失败只记录日志，不能影响数据库事务。
     */
    public void adjust(Long skuId, int delta) {
        if (delta == 0) {
            return;
        }
        try {
            redisTemplate.execute(ADJUST_SCRIPT, List.of(key(skuId)), String.valueOf(delta));
        } catch (RuntimeException e) {
            log.warn("Redis 库存调整失败，skuId={}，delta={}：{}", skuId, delta, e.getMessage());
        }
    }

    private String key(Long skuId) {
        return KEY_PREFIX + skuId;
    }

    /** 预扣结果 */
    public record DeductResult(boolean success, String message) {

        static DeductResult ok() {
            return new DeductResult(true, null);
        }
    }
}
