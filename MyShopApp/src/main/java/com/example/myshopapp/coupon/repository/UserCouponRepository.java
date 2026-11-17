package com.example.myshopapp.coupon.repository;

import com.example.myshopapp.coupon.entity.UserCoupon;
import com.example.myshopapp.coupon.entity.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    /** 我的券：连同券模板和店铺一起取，页面无需二次查询（平台券没有店铺，需左连接） */
    @Query("""
            select uc from UserCoupon uc
            join fetch uc.coupon c
            left join fetch c.shop
            where uc.user.id = :userId
            order by uc.id desc
            """)
    List<UserCoupon> findMine(@Param("userId") Long userId);

    /** 下单时按 id 批量取当前用户的券 */
    @Query("""
            select uc from UserCoupon uc
            join fetch uc.coupon c
            left join fetch c.shop
            where uc.id in :ids and uc.user.id = :userId
            """)
    List<UserCoupon> findByIdInAndUserId(@Param("ids") Collection<Long> ids,
                                         @Param("userId") Long userId);

    /** 订单取消时按订单找回用掉的券 */
    List<UserCoupon> findByOrderIdAndStatus(Long orderId, UserCouponStatus status);

    /** 即将到期且未使用的券，用于到期提醒 */
    @Query("""
            select uc from UserCoupon uc
            join fetch uc.coupon c
            where uc.status = :status
              and c.endAt > :now
              and c.endAt <= :deadline
            """)
    List<UserCoupon> findExpiring(@Param("status") UserCouponStatus status,
                                  @Param("now") LocalDateTime now,
                                  @Param("deadline") LocalDateTime deadline);

    /** 商家券列表统计已使用数量 */
    @Query("""
            select uc.coupon.id as couponId, count(uc) as total
            from UserCoupon uc
            where uc.coupon.shop.id = :shopId and uc.status = :status
            group by uc.coupon.id
            """)
    List<CouponUsedCount> countUsedByShop(@Param("shopId") Long shopId,
                                          @Param("status") UserCouponStatus status);

    /** 平台券列表统计已使用数量 */
    @Query("""
            select uc.coupon.id as couponId, count(uc) as total
            from UserCoupon uc
            where uc.coupon.id in :couponIds and uc.status = :status
            group by uc.coupon.id
            """)
    List<CouponUsedCount> countUsedByCouponIds(@Param("couponIds") Collection<Long> couponIds,
                                               @Param("status") UserCouponStatus status);

    interface CouponUsedCount {
        Long getCouponId();

        long getTotal();
    }
}
