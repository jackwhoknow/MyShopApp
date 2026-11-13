package com.example.myshopapp.coupon.dto;

import com.example.myshopapp.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 券管理视图（商家券 / 平台券共用）：state 由启停与有效期计算，
 * 取值为 RUNNING / NOT_STARTED / EXPIRED / DISABLED。
 */
public record MerchantCouponVO(
        Long id,
        Long shopId,
        String shopName,
        boolean platform,
        String title,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        int totalCount,
        int claimedCount,
        long usedCount,
        int remainingCount,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean enabled,
        String state,
        LocalDateTime createdAt
) {

    public static MerchantCouponVO from(Coupon coupon, long usedCount, LocalDateTime now) {
        String state;
        if (!coupon.isEnabled()) {
            state = "DISABLED";
        } else if (now.isBefore(coupon.getStartAt())) {
            state = "NOT_STARTED";
        } else if (!now.isBefore(coupon.getEndAt())) {
            state = "EXPIRED";
        } else {
            state = "RUNNING";
        }

        return new MerchantCouponVO(
                coupon.getId(),
                coupon.getShop() == null ? null : coupon.getShop().getId(),
                coupon.getShop() == null ? null : coupon.getShop().getName(),
                coupon.getShop() == null,
                coupon.getTitle(),
                coupon.getThresholdAmount(),
                coupon.getDiscountAmount(),
                coupon.getTotalCount(),
                coupon.getClaimedCount(),
                usedCount,
                Math.max(coupon.getTotalCount() - coupon.getClaimedCount(), 0),
                coupon.getStartAt(),
                coupon.getEndAt(),
                coupon.isEnabled(),
                state,
                coupon.getCreatedAt());
    }
}
