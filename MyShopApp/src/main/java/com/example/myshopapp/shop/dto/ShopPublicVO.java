package com.example.myshopapp.shop.dto;

import java.time.LocalDateTime;

/**
 * 店铺主页公开信息，只暴露买家可见字段，不含店主账号等敏感数据。
 */
public record ShopPublicVO(
        Long id,
        String name,
        String description,
        long productCount,
        long followers,
        LocalDateTime createdAt
) {
}
