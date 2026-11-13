package com.example.myshopapp.coupon.repository;

import com.example.myshopapp.coupon.entity.Coupon;
import com.example.myshopapp.shop.entity.ShopStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /** 可领取的券：启用、在有效期内、有剩余、且当前用户没领过；平台券不受店铺状态限制 */
    @Query("""
            select c from Coupon c
            left join c.shop s
            where c.enabled = true
              and c.startAt <= :now
              and c.endAt > :now
              and c.claimedCount < c.totalCount
              and (s is null or s.status = :shopStatus)
              and not exists (select 1 from UserCoupon uc
                              where uc.coupon = c and uc.user.id = :userId)
            order by c.discountAmount desc, c.id desc
            """)
    List<Coupon> findClaimable(@Param("userId") Long userId,
                               @Param("now") LocalDateTime now,
                               @Param("shopStatus") ShopStatus shopStatus);

    /** 领券时加悲观锁，避免并发把数量领超 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);

    List<Coupon> findByShopIdOrderByIdDesc(Long shopId);

    /** 平台券列表（shop 为空） */
    List<Coupon> findByShopIsNullOrderByIdDesc();

    Optional<Coupon> findByIdAndShopId(Long id, Long shopId);
}
