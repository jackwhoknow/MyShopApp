package com.example.myshopapp.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 商家调整店铺库存预警阈值。
 */
public record LowStockThresholdRequest(
        @NotNull(message = "请填写预警阈值")
        @Min(value = 0, message = "阈值不能为负数")
        @Max(value = 9999, message = "阈值不能超过 9999")
        Integer threshold
) {
}
