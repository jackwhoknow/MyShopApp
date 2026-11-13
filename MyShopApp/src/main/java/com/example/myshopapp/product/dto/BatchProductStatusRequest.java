package com.example.myshopapp.product.dto;

import com.example.myshopapp.product.entity.ProductStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量上下架入参：一次最多处理 100 个商品。
 */
public record BatchProductStatusRequest(

        @NotEmpty(message = "请选择要操作的商品")
        @Size(max = 100, message = "单次最多操作 100 个商品")
        List<Long> productIds,

        @NotNull(message = "目标状态不能为空")
        ProductStatus status
) {
}
