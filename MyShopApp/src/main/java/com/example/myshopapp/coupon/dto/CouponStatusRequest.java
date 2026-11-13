package com.example.myshopapp.coupon.dto;

import jakarta.validation.constraints.NotNull;

/** 商家启用/停用券 */
public record CouponStatusRequest(@NotNull(message = "请选择启用状态") Boolean enabled) {
}
