package com.example.myshopapp.product.dto;

import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductSpecValue;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 库存预警条目：展示 SKU 所属商品、规格与当前库存。
 */
public record StockAlertVO(
        Long skuId,
        Long productId,
        String productName,
        String image,
        String specDesc,
        BigDecimal price,
        int stock,
        String productStatus,
        int sales
) {

    public static StockAlertVO from(ProductSku sku) {
        String specDesc = sku.getSpecValues().stream()
                .sorted(Comparator.comparing((ProductSpecValue value) -> value.getSpec().getSortOrder())
                        .thenComparing(ProductSpecValue::getSortOrder))
                .map(ProductSpecValue::getValue)
                .collect(Collectors.joining(" / "));

        return new StockAlertVO(
                sku.getId(),
                sku.getProduct().getId(),
                sku.getProduct().getName(),
                sku.getImage() != null && !sku.getImage().isBlank()
                        ? sku.getImage() : sku.getProduct().getMainImage(),
                specDesc.isEmpty() ? "默认规格" : specDesc,
                sku.getPrice(),
                sku.getStock(),
                sku.getProduct().getStatus().name(),
                sku.getProduct().getSales());
    }
}
