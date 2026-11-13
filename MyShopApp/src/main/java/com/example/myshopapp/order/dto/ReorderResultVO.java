package com.example.myshopapp.order.dto;

import java.util.List;

/**
 * 一键复购结果：加入购物车的商品条数与因下架、售罄、库存不足被跳过的明细。
 */
public record ReorderResultVO(
        int addedCount,
        int skippedCount,
        List<SkippedItem> skippedItems
) {

    public record SkippedItem(
            String productName,
            String skuDesc,
            String reason
    ) {
    }
}
