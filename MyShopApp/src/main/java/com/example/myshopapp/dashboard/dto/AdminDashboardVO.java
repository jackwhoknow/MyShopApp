package com.example.myshopapp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 平台经营看板：基础规模、今日概况、近 N 天趋势与平台热销商品。
 */
public record AdminDashboardVO(
        long totalUsers,
        long activeShops,
        long pendingShops,
        long onShelfProducts,
        long totalOrders,
        BigDecimal totalSalesAmount,
        long todayOrderCount,
        BigDecimal todaySalesAmount,
        long refundingCount,
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
