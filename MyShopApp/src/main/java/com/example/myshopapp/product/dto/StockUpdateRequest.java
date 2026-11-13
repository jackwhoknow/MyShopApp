package com.example.myshopapp.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 库存预警页快速补货入参：直接设置 SKU 的新库存。
 */
public record StockUpdateRequest(
        @NotNull(message = "请填写库存")
        @Min(value = 0, message = "库存不能为负数")
        @Max(value = 999999, message = "库存不能超过 999999")
        Integer stock
) {
}
