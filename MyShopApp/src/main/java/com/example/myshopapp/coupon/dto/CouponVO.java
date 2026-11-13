package com.example.myshopapp.coupon.dto;

import com.example.myshopapp.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可领取券视图：买家领券中心使用，remainingCount 便于页面提示剩余数量。
 * platform 为 true 时 shopId / shopName 为空。
 */
public record CouponVO(
        Long id,
        Long shopId,
        String shopName,
        boolean platform,
        String title,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        int remainingCount,
        LocalDateTime startAt,
        LocalDateTime endAt
) {

    public static CouponVO from(Coupon coupon) {
        return new CouponVO(
                coupon.getId(),
                coupon.getShop() == null ? null : coupon.getShop().getId(),
                coupon.getShop() == null ? null : coupon.getShop().getName(),
                coupon.getShop() == null,
                coupon.getTitle(),
                coupon.getThresholdAmount(),
                coupon.getDiscountAmount(),
                Math.max(coupon.getTotalCount() - coupon.getClaimedCount(), 0),
                coupon.getStartAt(),
                coupon.getEndAt());
    }
}
