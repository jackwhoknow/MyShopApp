package com.example.myshopapp.product.dto;

import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductSpec;
import com.example.myshopapp.product.entity.ProductSpecValue;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 商品详情：包含完整规格与 SKU，供商品详情页选择规格使用。
 */
public record ProductDetailVO(
        Long id,
        String name,
        String subtitle,
        String description,
        String mainImage,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int sales,
        int purchaseLimit,
        String status,
        Long shopId,
        String shopName,
        Long categoryId,
        String categoryName,
        /** 审核驳回理由，未驳回时为 null */
        String rejectReason,
        List<SpecVO> specs,
        List<SkuVO> skus
) {

    public static ProductDetailVO from(Product product) {
        List<SpecVO> specs = product.getSpecs().stream()
                .sorted(Comparator.comparingInt(ProductSpec::getSortOrder).thenComparing(ProductSpec::getId))
                .map(SpecVO::from)
                .toList();

        List<SkuVO> skus = product.getSkus().stream()
                .sorted(Comparator.comparing(ProductSku::getId))
                .map(SkuVO::from)
                .toList();

        return new ProductDetailVO(
                product.getId(),
                product.getName(),
                product.getSubtitle(),
                product.getDescription(),
                product.getMainImage(),
                product.getMinPrice(),
                product.getMaxPrice(),
                product.getSales(),
                product.getPurchaseLimit(),
                product.getStatus().name(),
                product.getShop().getId(),
                product.getShop().getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getRejectReason(),
                specs,
                skus);
    }

    public record SpecVO(Long id, String name, List<ValueVO> values) {

        static SpecVO from(ProductSpec spec) {
            return new SpecVO(
                    spec.getId(),
                    spec.getName(),
                    spec.getValues().stream()
                            .sorted(Comparator.comparingInt(ProductSpecValue::getSortOrder)
                                    .thenComparing(ProductSpecValue::getId))
                            .map(v -> new ValueVO(v.getId(), v.getValue()))
                            .toList());
        }
    }

    public record ValueVO(Long id, String value) {
    }

    public record SkuVO(Long id, BigDecimal price, int stock, String image, List<SkuSpecVO> specValues) {

        static SkuVO from(ProductSku sku) {
            List<SkuSpecVO> values = sku.getSpecValues().stream()
                    .sorted(Comparator.comparing(v -> v.getSpec().getSortOrder()))
                    .map(v -> new SkuSpecVO(v.getId(), v.getSpec().getName(), v.getValue()))
                    .toList();
            return new SkuVO(sku.getId(), sku.getPrice(), sku.getStock(), sku.getImage(), values);
        }
    }

    public record SkuSpecVO(Long specValueId, String specName, String value) {
    }
}
