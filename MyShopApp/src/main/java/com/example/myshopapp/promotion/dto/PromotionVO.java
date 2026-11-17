package com.example.myshopapp.promotion.dto;

import com.example.myshopapp.promotion.entity.ShopPromotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 满减活动视图。
 */
public record PromotionVO(
        Long id,
        Long shopId,
        String shopName,
        String title,
        BigDecimal thresholdAmount,
        BigDecimal reduceAmount,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean enabled,
        boolean running
) {

    public static PromotionVO from(ShopPromotion promotion, LocalDateTime now) {
        boolean running = promotion.isEnabled()
                && !now.isBefore(promotion.getStartAt())
                && now.isBefore(promotion.getEndAt());
        return new PromotionVO(
                promotion.getId(),
                promotion.getShop().getId(),
                promotion.getShop().getName(),
                promotion.getTitle(),
                promotion.getThresholdAmount(),
                promotion.getReduceAmount(),
                promotion.getStartAt(),
                promotion.getEndAt(),
                promotion.isEnabled(),
                running);
    }
}
