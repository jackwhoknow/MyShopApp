package com.example.myshopapp.promotion.repository;

import com.example.myshopapp.promotion.entity.ShopPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShopPromotionRepository extends JpaRepository<ShopPromotion, Long> {

    List<ShopPromotion> findByShopIdOrderByIdDesc(Long shopId);

    Optional<ShopPromotion> findByIdAndShopId(Long id, Long shopId);

    /** 进行中的活动，按优惠金额倒序，下单时取第一个满足门槛的即可 */
    @Query("""
            select p from ShopPromotion p
            where p.shop.id = :shopId
              and p.enabled = true
              and p.startAt <= :now
              and p.endAt > :now
            order by p.reduceAmount desc, p.id desc
            """)
    List<ShopPromotion> findActive(@Param("shopId") Long shopId, @Param("now") LocalDateTime now);
}
