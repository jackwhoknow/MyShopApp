package com.example.myshopapp.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家维护满减活动：门槛与优惠金额必须大于 0。
 */
public record PromotionRequest(
        @NotBlank(message = "活动名称不能为空")
        @Size(max = 50, message = "活动名称不能超过 50 个字符")
        String title,

        @NotNull(message = "请填写满减门槛")
        @DecimalMin(value = "0.01", message = "满减门槛必须大于 0")
        BigDecimal thresholdAmount,

        @NotNull(message = "请填写优惠金额")
        @DecimalMin(value = "0.01", message = "优惠金额必须大于 0")
        BigDecimal reduceAmount,

        @NotNull(message = "请选择开始时间")
        LocalDateTime startAt,

        @NotNull(message = "请选择结束时间")
        LocalDateTime endAt,

        Boolean enabled
) {
}
