package com.example.myshopapp.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品修改入参：specs 与 skus 都不传时只改基础信息；
 * 两个都传时按提交内容全量替换规格与 SKU（带 id 的 SKU 复用原记录）。
 */
public record ProductUpdateRequest(

        Long categoryId,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称不能超过 100 个字符")
        String name,

        @Size(max = 200, message = "副标题不能超过 200 个字符")
        String subtitle,

        String description,

        @Size(max = 255, message = "主图地址不能超过 255 个字符")
        String mainImage,

        /** 为空表示不修改规格 */
        @Valid
        List<SpecRequest> specs,

        /** 为空表示不修改 SKU */
        @Valid
        List<SkuRequest> skus
) {

    public record SpecRequest(
            @NotBlank(message = "规格名称不能为空")
            @Size(max = 50, message = "规格名称不能超过 50 个字符")
            String name,

            @NotEmpty(message = "规格至少有一个取值")
            List<@NotBlank(message = "规格值不能为空") String> values
    ) {
    }

    public record SkuRequest(
            /** 已有 SKU 传 id，新 SKU 不传 */
            Long id,

            @NotNull(message = "SKU 价格不能为空")
            @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
            BigDecimal price,

            @NotNull(message = "SKU 库存不能为空")
            @Min(value = 0, message = "库存不能为负数")
            Integer stock,

            @Size(max = 255, message = "SKU 图片地址不能超过 255 个字符")
            String image,

            /** key = 规格名称，value = 规格值，例如 {"颜色":"红色"} */
            @NotEmpty(message = "SKU 必须指定规格取值")
            Map<String, String> specValues
    ) {
    }
}
