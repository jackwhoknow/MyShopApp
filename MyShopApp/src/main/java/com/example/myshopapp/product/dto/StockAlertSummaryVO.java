package com.example.myshopapp.product.dto;

/**
 * 库存预警概览：缺货、低库存与当前阈值。
 */
public record StockAlertSummaryVO(
        long outOfStockCount,
        long lowStockCount,
        long totalSkuCount,
        int threshold
) {
}
