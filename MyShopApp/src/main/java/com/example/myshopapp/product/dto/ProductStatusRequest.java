package com.example.myshopapp.product.dto;

import com.example.myshopapp.product.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 上架 / 下架入参。
 */
public record ProductStatusRequest(
        @NotNull(message = "目标状态不能为空")
        ProductStatus status
) {
}
