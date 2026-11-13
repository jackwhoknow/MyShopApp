package com.example.myshopapp.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 价格与库存维护入参：只改价格和库存，不动规格组合。
 */
public record SkuStockUpdateRequest(

        @NotEmpty(message = "请至少提交一个 SKU")
        @Valid
        List<Item> skus
) {

    public record Item(
            @NotNull(message = "SKU 标识不能为空")
            Long skuId,

            @NotNull(message = "SKU 价格不能为空")
            @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
            BigDecimal price,

            @NotNull(message = "SKU 库存不能为空")
            @Min(value = 0, message = "库存不能为负数")
            Integer stock
    ) {
    }
}
