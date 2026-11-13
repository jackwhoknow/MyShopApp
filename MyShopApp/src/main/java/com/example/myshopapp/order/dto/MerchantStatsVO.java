package com.example.myshopapp.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商家经营看板：今日概况 + 累计数据 + 近 N 天趋势 + 热销商品。
 */
public record MerchantStatsVO(
        long todayOrderCount,
        BigDecimal todaySalesAmount,
        long pendingShipCount,
        long refundingCount,
        BigDecimal totalSalesAmount,
        int days,
        List<TrendPoint> trend,
        List<TopProduct> topProducts
) {

    /** 趋势点：某一天的销售金额与订单数，无数据的天补 0 */
    public record TrendPoint(LocalDate date, long orderCount, BigDecimal salesAmount) {
    }

    /** 热销商品：销量与销售额都来自订单快照 */
    public record TopProduct(Long productId, String productName, long quantity, BigDecimal salesAmount) {
    }
}
