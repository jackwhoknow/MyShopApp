package com.example.myshopapp.product.dto;

import com.example.myshopapp.product.entity.Product;

import java.math.BigDecimal;

/**
 * 商品列表项，列表页不需要 SKU 明细。
 */
public record ProductListItemVO(
        Long id,
        String name,
        String subtitle,
        String mainImage,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int sales,
        String status,
        Long shopId,
        String shopName,
        Long categoryId,
        /** 审核驳回理由，未驳回时为 null */
        String rejectReason
) {

    public static ProductListItemVO from(Product product) {
        return new ProductListItemVO(
                product.getId(),
                product.getName(),
                product.getSubtitle(),
                product.getMainImage(),
                product.getMinPrice(),
                product.getMaxPrice(),
                product.getSales(),
                product.getStatus().name(),
                product.getShop().getId(),
                product.getShop().getName(),
                product.getCategory().getId(),
                product.getRejectReason());
    }
}
