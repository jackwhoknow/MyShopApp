package com.example.myshopapp.cart.dto;

import com.example.myshopapp.cart.entity.CartItem;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.support.SkuDescriber;
import com.example.myshopapp.shop.entity.ShopStatus;

import java.math.BigDecimal;

/**
 * 购物车展示项。价格、库存实时读取 SKU；available 表示当前数量是否还能下单。
 */
public record CartItemVO(
        Long id,
        Long skuId,
        Long productId,
        String productName,
        String skuDesc,
        String image,
        BigDecimal price,
        int quantity,
        int stock,
        boolean available,
        Long shopId,
        String shopName
) {

    public static CartItemVO from(CartItem item) {
        ProductSku sku = item.getSku();
        Product product = sku.getProduct();

        boolean available = product.getStatus() == ProductStatus.ON_SHELF
                && product.getShop().getStatus() == ShopStatus.ACTIVE
                && sku.getStock() >= item.getQuantity();

        String image = sku.getImage() == null || sku.getImage().isBlank()
                ? product.getMainImage() : sku.getImage();

        return new CartItemVO(
                item.getId(),
                sku.getId(),
                product.getId(),
                product.getName(),
                SkuDescriber.describe(sku),
                image,
                sku.getPrice(),
                item.getQuantity(),
                sku.getStock(),
                available,
                product.getShop().getId(),
                product.getShop().getName());
    }
}
