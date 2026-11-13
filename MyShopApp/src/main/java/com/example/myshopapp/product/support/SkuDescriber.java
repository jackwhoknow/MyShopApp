package com.example.myshopapp.product.support;

import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductSpecValue;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 把 SKU 的规格值拼成可读文案，例如“黑色 / 256GB”。
 * 购物车和订单快照都复用这段逻辑，保证展示一致。
 */
public final class SkuDescriber {

    private SkuDescriber() {
    }

    public static String describe(ProductSku sku) {
        return sku.getSpecValues().stream()
                .sorted(Comparator.comparingInt(v -> v.getSpec().getSortOrder()))
                .map(ProductSpecValue::getValue)
                .collect(Collectors.joining(" / "));
    }
}
