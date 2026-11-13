package com.example.myshopapp.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 加入购物车：同一个 SKU 重复加购会合并数量。
 */
public record CartAddRequest(
        @NotNull(message = "SKU 不能为空") Long skuId,
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单个商品最多 99 件") Integer quantity
) {
}
