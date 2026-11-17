package com.example.myshopapp.seckill.dto;

import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.support.SkuDescriber;
import com.example.myshopapp.seckill.entity.SeckillActivity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动视图：state 由启停、时间与库存实时计算，
 * 取值为 NOT_STARTED / RUNNING / SOLD_OUT / EXPIRED / DISABLED。
 */
public record SeckillActivityVO(
        Long id,
        Long shopId,
        String shopName,
        Long productId,
        String productName,
        Long skuId,
        String skuDesc,
        String image,
        BigDecimal originalPrice,
        BigDecimal activityPrice,
        int totalStock,
        int soldCount,
        int remainingStock,
        int perUserLimit,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean enabled,
        String state
) {

    public static SeckillActivityVO from(SeckillActivity activity, LocalDateTime now) {
        ProductSku sku = activity.getSku();
        Product product = activity.getProduct();
        int remaining = Math.max(activity.getTotalStock() - activity.getSoldCount(), 0);

        String state;
        if (!activity.isEnabled()) {
            state = "DISABLED";
        } else if (!now.isBefore(activity.getEndAt())) {
            state = "EXPIRED";
        } else if (now.isBefore(activity.getStartAt())) {
            state = "NOT_STARTED";
        } else if (remaining <= 0) {
            state = "SOLD_OUT";
        } else {
            state = "RUNNING";
        }

        String image = sku.getImage() == null || sku.getImage().isBlank()
                ? product.getMainImage() : sku.getImage();

        return new SeckillActivityVO(
                activity.getId(),
                activity.getShop().getId(),
                activity.getShop().getName(),
                product.getId(),
                product.getName(),
                sku.getId(),
                SkuDescriber.describe(sku),
                image,
                sku.getPrice(),
                activity.getActivityPrice(),
                activity.getTotalStock(),
                activity.getSoldCount(),
                remaining,
                activity.getPerUserLimit(),
                activity.getStartAt(),
                activity.getEndAt(),
                activity.isEnabled(),
                state);
    }
}
