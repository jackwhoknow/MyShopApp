package com.example.myshopapp.favorite.dto;

import com.example.myshopapp.favorite.entity.ProductFavorite;
import com.example.myshopapp.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的收藏列表项：商品下架后仍返回，并带上商品状态供前端提示。
 */
public record FavoriteVO(
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
        LocalDateTime favoritedAt
) {

    public static FavoriteVO from(ProductFavorite favorite) {
        Product product = favorite.getProduct();
        return new FavoriteVO(
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
                favorite.getCreatedAt());
    }
}
