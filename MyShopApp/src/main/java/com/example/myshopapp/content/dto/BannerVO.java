package com.example.myshopapp.content.dto;

import com.example.myshopapp.content.entity.Banner;

import java.time.LocalDateTime;

public record BannerVO(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        Integer sortOrder,
        boolean enabled,
        LocalDateTime createdAt) {

    public static BannerVO from(Banner banner) {
        return new BannerVO(banner.getId(), banner.getTitle(), banner.getImageUrl(),
                banner.getLinkUrl(), banner.getSortOrder(), banner.isEnabled(), banner.getCreatedAt());
    }
}
