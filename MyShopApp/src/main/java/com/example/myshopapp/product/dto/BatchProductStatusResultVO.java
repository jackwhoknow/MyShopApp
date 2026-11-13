package com.example.myshopapp.product.dto;

import java.util.List;

/**
 * 批量上下架结果：成功数量与失败明细，失败的商品不影响其余商品。
 */
public record BatchProductStatusResultVO(
        int total,
        int updated,
        List<Failure> failures
) {

    public record Failure(Long productId, String productName, String reason) {
    }
}
