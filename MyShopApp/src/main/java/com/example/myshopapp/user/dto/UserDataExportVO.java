package com.example.myshopapp.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人数据导出：用户可下载自己在平台产生的资料、订单、评价、优惠券与积分记录。
 */
public record UserDataExportVO(
        LocalDateTime exportedAt,
        UserVO profile,
        List<AddressItem> addresses,
        List<OrderItem> orders,
        List<ReviewItem> reviews,
        List<CouponItem> coupons,
        PointsSummary points
) {

    public record AddressItem(String receiverName, String phone, String province, String city,
                              String district, String detail, boolean defaultAddress) {
    }

    public record OrderItem(String orderNo, BigDecimal totalAmount, String status,
                            int pointsUsed, int pointsEarned,
                            LocalDateTime createdAt, LocalDateTime paidAt) {
    }

    public record ReviewItem(Long id, Long productId, int rating, String content, String images,
                             String status, LocalDateTime createdAt) {
    }

    public record CouponItem(String title, BigDecimal thresholdAmount, BigDecimal discountAmount,
                             String status, LocalDateTime claimedAt, LocalDateTime usedAt,
                             LocalDateTime endAt) {
    }

    public record PointsSummary(int balance, int totalEarned, int totalSpent,
                                List<PointItem> transactions) {
    }

    public record PointItem(int changeAmount, int balanceAfter, String type, String orderNo,
                            String remark, LocalDateTime createdAt) {
    }
}
