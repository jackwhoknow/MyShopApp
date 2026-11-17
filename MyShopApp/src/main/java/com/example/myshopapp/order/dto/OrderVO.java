package com.example.myshopapp.order.dto;

import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 订单视图：主订单 + 按店铺拆分的子订单 + 商品快照，列表与详情共用。
 */
public record OrderVO(
        Long id,
        String orderNo,
        BigDecimal totalAmount,
        int pointsUsed,
        BigDecimal pointsDiscount,
        int pointsEarned,
        String status,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        List<SubOrderVO> subOrders
) {

    public static OrderVO from(Order order) {
        List<SubOrderVO> subOrders = order.getSubOrders().stream()
                .sorted(Comparator.comparing(SubOrder::getId))
                .map(SubOrderVO::from)
                .toList();

        return new OrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getPointsUsed(),
                order.getPointsDiscount(),
                order.getPointsEarned(),
                order.getStatus().name(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getRemark(),
                order.getCreatedAt(),
                order.getPaidAt(),
                subOrders);
    }

    public record SubOrderVO(
            Long id,
            String subOrderNo,
            Long shopId,
            String shopName,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            String couponTitle,
            BigDecimal promotionDiscount,
            String promotionTitle,
            String status,
            String shipCompany,
            String trackingNo,
            LocalDateTime shippedAt,
            LocalDateTime completedAt,
            List<OrderItemVO> items
    ) {

        static SubOrderVO from(SubOrder subOrder) {
            List<OrderItemVO> items = subOrder.getItems().stream()
                    .sorted(Comparator.comparing(OrderItem::getId))
                    .map(OrderItemVO::from)
                    .toList();

            return new SubOrderVO(
                    subOrder.getId(),
                    subOrder.getSubOrderNo(),
                    subOrder.getShop().getId(),
                    subOrder.getShop().getName(),
                    subOrder.getTotalAmount(),
                    subOrder.getDiscountAmount(),
                    subOrder.getCouponTitle(),
                    subOrder.getPromotionDiscount(),
                    subOrder.getPromotionTitle(),
                    subOrder.getStatus().name(),
                    subOrder.getShipCompany(),
                    subOrder.getTrackingNo(),
                    subOrder.getShippedAt(),
                    subOrder.getCompletedAt(),
                    items);
        }
    }

    public record OrderItemVO(
            Long id,
            Long productId,
            Long skuId,
            String productName,
            String skuDesc,
            String image,
            BigDecimal price,
            int quantity,
            BigDecimal totalPrice
    ) {

        static OrderItemVO from(OrderItem item) {
            return new OrderItemVO(
                    item.getId(),
                    item.getProductId(),
                    item.getSkuId(),
                    item.getProductName(),
                    item.getSkuDesc(),
                    item.getImage(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getTotalPrice());
        }
    }
}
