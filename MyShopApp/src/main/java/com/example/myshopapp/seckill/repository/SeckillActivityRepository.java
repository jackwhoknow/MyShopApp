package com.example.myshopapp.seckill.repository;

import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.seckill.entity.SeckillActivity;
import com.example.myshopapp.shop.entity.ShopStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeckillActivityRepository extends JpaRepository<SeckillActivity, Long> {

    List<SeckillActivity> findByShopIdOrderByIdDesc(Long shopId);

    Optional<SeckillActivity> findByIdAndShopId(Long id, Long shopId);

    /** 下单扣减活动库存时加悲观锁，避免并发超卖 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SeckillActivity a where a.id = :id")
    Optional<SeckillActivity> findByIdForUpdate(@Param("id") Long id);

    /** 正在进行的活动：启用、窗口内、商品在售、店铺正常 */
    @Query("""
            select a from SeckillActivity a
            join fetch a.sku s
            join fetch s.product p
            join fetch p.shop
            where a.enabled = true
              and a.startAt <= :now
              and a.endAt > :now
              and p.status = :productStatus
              and p.shop.status = :shopStatus
            order by a.id asc
            """)
    List<SeckillActivity> findRunning(@Param("now") LocalDateTime now,
                                      @Param("productStatus") ProductStatus productStatus,
                                      @Param("shopStatus") ShopStatus shopStatus);

    /** 指定 SKU 正在进行的活动，购物车展示活动价使用 */
    @Query("""
            select a from SeckillActivity a
            where a.enabled = true
              and a.startAt <= :now
              and a.endAt > :now
              and a.sku.id in :skuIds
            order by a.id asc
            """)
    List<SeckillActivity> findRunningBySkuIds(@Param("skuIds") Collection<Long> skuIds,
                                              @Param("now") LocalDateTime now);

    /** 首页秒杀专区：进行中 + 即将开始 */
    @Query("""
            select a from SeckillActivity a
            join fetch a.sku s
            join fetch s.product p
            join fetch p.shop
            where a.enabled = true
              and a.endAt > :now
              and p.status = :productStatus
              and p.shop.status = :shopStatus
            order by a.startAt asc, a.id asc
            """)
    List<SeckillActivity> findUpcomingOrRunning(@Param("now") LocalDateTime now,
                                                @Param("productStatus") ProductStatus productStatus,
                                                @Param("shopStatus") ShopStatus shopStatus);

    /** 同一 SKU 的活动时间不能重叠，保证任意时刻只有一个活动价 */
    @Query("""
            select count(a) > 0 from SeckillActivity a
            where a.sku.id = :skuId
              and a.enabled = true
              and a.startAt < :endAt
              and a.endAt > :startAt
              and a.id <> :excludeId
            """)
    boolean existsOverlapping(@Param("skuId") Long skuId,
                              @Param("startAt") LocalDateTime startAt,
                              @Param("endAt") LocalDateTime endAt,
                              @Param("excludeId") Long excludeId);
}
