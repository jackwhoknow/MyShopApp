package com.example.myshopapp.history.dto;

import com.example.myshopapp.history.entity.BrowseHistory;
import com.example.myshopapp.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 最近浏览列表项：商品下架后仍返回，并带上商品状态供前端提示。
 */
public record BrowseHistoryVO(
        Long productId,
        String name,
        String subtitle,
        String mainImage,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int sales,
        String status,
        Long shopId,
        String shopName,
        LocalDateTime viewedAt
) {

    public static BrowseHistoryVO from(BrowseHistory history) {
        Product product = history.getProduct();
        return new BrowseHistoryVO(
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
                history.getViewedAt());
    }
}
