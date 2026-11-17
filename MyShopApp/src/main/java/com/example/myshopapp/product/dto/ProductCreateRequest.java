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
 * 商品创建入参：先声明规格，再按“规格名 -> 规格值”组合出 SKU。
 */
public record ProductCreateRequest(

        @NotNull(message = "必须选择分类")
        Long categoryId,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称不能超过 100 个字符")
        String name,

        @Size(max = 200, message = "副标题不能超过 200 个字符")
        String subtitle,

        String description,

        @NotBlank(message = "主图不能为空")
        @Size(max = 255, message = "主图地址不能超过 255 个字符")
        String mainImage,

        /** 每个用户限购数量，0 或不传表示不限购 */
        @Min(value = 0, message = "限购数量不能为负数")
        Integer purchaseLimit,

        @NotEmpty(message = "至少定义一个规格")
        @Valid
        List<SpecRequest> specs,

        @NotEmpty(message = "至少创建一个 SKU")
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
