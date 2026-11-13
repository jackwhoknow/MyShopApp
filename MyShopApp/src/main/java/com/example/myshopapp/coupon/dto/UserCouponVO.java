package com.example.myshopapp.coupon.dto;

import com.example.myshopapp.coupon.entity.UserCoupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的券视图：state 由状态与有效期实时计算，取值为 UNUSED / USED / EXPIRED。
 * platform 为 true 时 shopId / shopName 为空。
 */
public record UserCouponVO(
        Long id,
        Long couponId,
        Long shopId,
        String shopName,
        boolean platform,
        String title,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        String state,
        Long orderId,
        LocalDateTime claimedAt,
        LocalDateTime usedAt,
        LocalDateTime endAt
) {

    public static UserCouponVO from(UserCoupon userCoupon, LocalDateTime now) {
        String state;
        if (userCoupon.getStatus() == com.example.myshopapp.coupon.entity.UserCouponStatus.USED) {
            state = "USED";
        } else if (!now.isBefore(userCoupon.getCoupon().getEndAt())) {
            state = "EXPIRED";
        } else {
            state = "UNUSED";
        }

        return new UserCouponVO(
                userCoupon.getId(),
                userCoupon.getCoupon().getId(),
                userCoupon.getCoupon().getShop() == null ? null : userCoupon.getCoupon().getShop().getId(),
                userCoupon.getCoupon().getShop() == null ? null : userCoupon.getCoupon().getShop().getName(),
                userCoupon.getCoupon().getShop() == null,
                userCoupon.getCoupon().getTitle(),
                userCoupon.getCoupon().getThresholdAmount(),
                userCoupon.getCoupon().getDiscountAmount(),
                state,
                userCoupon.getOrderId(),
                userCoupon.getClaimedAt(),
                userCoupon.getUsedAt(),
                userCoupon.getCoupon().getEndAt());
    }
}
