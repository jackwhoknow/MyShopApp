package com.example.myshopapp.shop.dto;

/**
 * 我的关注店铺展示对象。
 */
public record FollowedShopVO(Long shopId, String name, String description, String status,
                             java.time.LocalDateTime followedAt) {
}
