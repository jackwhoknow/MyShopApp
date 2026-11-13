package com.example.myshopapp.shop.dto;

import com.example.myshopapp.shop.entity.Shop;

import java.time.LocalDateTime;

/**
 * 店铺对外展示结构。
 */
public record ShopVO(
        Long id,
        Long ownerId,
        String ownerUsername,
        String name,
        String description,
        String status,
        String rejectReason,
        LocalDateTime createdAt
) {

    public static ShopVO from(Shop shop) {
        return new ShopVO(
                shop.getId(),
                shop.getOwner().getId(),
                shop.getOwner().getUsername(),
                shop.getName(),
                shop.getDescription(),
                shop.getStatus().name(),
                shop.getRejectReason(),
                shop.getCreatedAt());
    }
}
