package com.example.myshopapp.cart.dto;

import com.example.myshopapp.cart.entity.CartItem;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.support.SkuDescriber;
import com.example.myshopapp.seckill.entity.SeckillActivity;
import com.example.myshopapp.shop.entity.ShopStatus;

import java.math.BigDecimal;

/**
 * 购物车展示项。价格、库存实时读取 SKU；命中秒杀活动时 price 为活动价，
 * originalPrice 为 SKU 原价，seckill 标识是否按活动价展示。
 */
public record CartItemVO(
        Long id,
        Long skuId,
        Long productId,
        String productName,
        String skuDesc,
        String image,
        BigDecimal price,
        BigDecimal originalPrice,
        int quantity,
        int stock,
        boolean available,
        boolean seckill,
        Long shopId,
        String shopName
) {

    public static CartItemVO from(CartItem item) {
        return from(item, null);
    }

    public static CartItemVO from(CartItem item, SeckillActivity activity) {
        ProductSku sku = item.getSku();
        Product product = sku.getProduct();

        boolean available = product.getStatus() == ProductStatus.ON_SHELF
                && product.getShop().getStatus() == ShopStatus.ACTIVE
                && sku.getStock() >= item.getQuantity();

        String image = sku.getImage() == null || sku.getImage().isBlank()
                ? product.getMainImage() : sku.getImage();

        BigDecimal price = activity == null ? sku.getPrice() : activity.getActivityPrice();

        return new CartItemVO(
                item.getId(),
                sku.getId(),
                product.getId(),
                product.getName(),
                SkuDescriber.describe(sku),
                image,
                price,
                sku.getPrice(),
                item.getQuantity(),
                sku.getStock(),
                available,
                activity != null,
                product.getShop().getId(),
                product.getShop().getName());
    }
}
