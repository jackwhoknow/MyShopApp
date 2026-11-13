package com.example.myshopapp.order.dto;

import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 商家端子订单视图：包含买家收货快照和商品明细，商家无需再查主订单。
 */
public record MerchantSubOrderVO(
        Long id,
        String subOrderNo,
        Long shopId,
        String shopName,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String couponTitle,
        String status,
        LocalDateTime createdAt,
        LocalDateTime shippedAt,
        LocalDateTime completedAt,
        String shipCompany,
        String trackingNo,
        Long orderId,
        String orderNo,
        String orderStatus,
        LocalDateTime paidAt,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String remark,
        String merchantRemark,
        List<ItemVO> items
) {

    public static MerchantSubOrderVO from(SubOrder subOrder) {
        List<ItemVO> items = subOrder.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .map(ItemVO::from)
                .toList();

        return new MerchantSubOrderVO(
                subOrder.getId(),
                subOrder.getSubOrderNo(),
                subOrder.getShop().getId(),
                subOrder.getShop().getName(),
                subOrder.getTotalAmount(),
                subOrder.getDiscountAmount(),
                subOrder.getCouponTitle(),
                subOrder.getStatus().name(),
                subOrder.getCreatedAt(),
                subOrder.getShippedAt(),
                subOrder.getCompletedAt(),
                subOrder.getShipCompany(),
                subOrder.getTrackingNo(),
                subOrder.getOrder().getId(),
                subOrder.getOrder().getOrderNo(),
                subOrder.getOrder().getStatus().name(),
                subOrder.getOrder().getPaidAt(),
                subOrder.getOrder().getReceiverName(),
                subOrder.getOrder().getReceiverPhone(),
                subOrder.getOrder().getReceiverAddress(),
                subOrder.getOrder().getRemark(),
                subOrder.getMerchantRemark(),
                items);
    }

    public record ItemVO(
            Long productId,
            Long skuId,
            String productName,
            String skuDesc,
            String image,
            BigDecimal price,
            int quantity,
            BigDecimal totalPrice
    ) {

        static ItemVO from(OrderItem item) {
            return new ItemVO(
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
