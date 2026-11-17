package com.example.myshopapp.points.dto;

import com.example.myshopapp.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分兑换券视图：pointsPrice 为兑换所需积分，exchanged 表示当前用户是否已兑换。
 */
public record PointCouponVO(
        Long id,
        Long shopId,
        String shopName,
        boolean platform,
        String title,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        int pointsPrice,
        int remainingCount,
        LocalDateTime endAt,
        boolean exchanged
) {

    public static PointCouponVO from(Coupon coupon, boolean exchanged) {
        return new PointCouponVO(
                coupon.getId(),
                coupon.getShop() == null ? null : coupon.getShop().getId(),
                coupon.getShop() == null ? null : coupon.getShop().getName(),
                coupon.getShop() == null,
                coupon.getTitle(),
                coupon.getThresholdAmount(),
                coupon.getDiscountAmount(),
                coupon.getPointsPrice(),
                Math.max(coupon.getTotalCount() - coupon.getClaimedCount(), 0),
                coupon.getEndAt(),
                exchanged);
    }
}
