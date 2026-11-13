package com.example.myshopapp.coupon.entity;

/**
 * 用户领券记录状态：未使用、已使用。过期状态按券有效期实时计算，不落库。
 */
public enum UserCouponStatus {

    UNUSED,
    USED
}
