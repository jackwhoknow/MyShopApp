package com.example.myshopapp.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家创建满减券：减免金额需小于门槛，由服务层校验。
 */
public record CouponCreateRequest(
        @NotBlank(message = "请填写券名称") @Size(max = 50, message = "券名称不能超过 50 字") String title,
        @NotNull(message = "请填写使用门槛") @DecimalMin(value = "0.01", message = "使用门槛需大于 0") BigDecimal thresholdAmount,
        @NotNull(message = "请填写减免金额") @DecimalMin(value = "0.01", message = "减免金额需大于 0") BigDecimal discountAmount,
        @NotNull(message = "请填写发放数量") @Min(value = 1, message = "发放数量至少为 1") Integer totalCount,
        @NotNull(message = "请选择开始时间") LocalDateTime startAt,
        @NotNull(message = "请选择结束时间") LocalDateTime endAt
) {
}
