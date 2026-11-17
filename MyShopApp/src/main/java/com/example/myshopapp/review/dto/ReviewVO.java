package com.example.myshopapp.review.dto;

import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.review.entity.Review;
import com.example.myshopapp.review.entity.ReviewStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 评价展示对象：买家与商家共用，含商品快照与订单号。
 */
public record ReviewVO(
        Long id,
        Long orderItemId,
        String orderNo,
        String subOrderNo,
        Long productId,
        String productName,
        String skuDesc,
        String image,
        int rating,
        String content,
        List<String> images,
        String appendContent,
        List<String> appendImages,
        LocalDateTime appendedAt,
        String reply,
        LocalDateTime repliedAt,
        ReviewStatus status,
        String auditRemark,
        LocalDateTime createdAt
) {

    public static ReviewVO from(Review review) {
        OrderItem item = review.getOrderItem();
        SubOrder subOrder = item.getSubOrder();
        return new ReviewVO(
                review.getId(),
                item.getId(),
                subOrder.getOrder().getOrderNo(),
                subOrder.getSubOrderNo(),
                item.getProductId(),
                item.getProductName(),
                item.getSkuDesc(),
                item.getImage(),
                review.getRating(),
                review.getContent(),
                review.getImages() == null || review.getImages().isBlank()
                        ? List.of()
                        : Arrays.stream(review.getImages().split(",")).filter(s -> !s.isBlank()).toList(),
                review.getAppendContent(),
                review.getAppendImages() == null || review.getAppendImages().isBlank()
                        ? List.of()
                        : Arrays.stream(review.getAppendImages().split(",")).filter(s -> !s.isBlank()).toList(),
                review.getAppendedAt(),
                review.getReply(),
                review.getRepliedAt(),
                review.getStatus(),
                review.getAuditRemark(),
                review.getCreatedAt());
    }
}
