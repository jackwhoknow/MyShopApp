package com.example.myshopapp.order.dto;

import com.example.myshopapp.order.entity.AfterSale;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 平台端订单详情视图：包含买家、收货信息、子订单明细、物流与售后状态。
 * 管理员只读查看，不做改单操作。
 */
public record AdminOrderDetailVO(
        Long id,
        String orderNo,
        BigDecimal totalAmount,
        String status,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        Long buyerId,
        String buyerUsername,
        String buyerNickname,
        String buyerPhone,
        List<SubOrderDetail> subOrders
) {

    public static AdminOrderDetailVO from(Order order, Map<Long, AfterSale> latestAfterSales) {
        List<SubOrderDetail> subOrders = order.getSubOrders().stream()
                .sorted(Comparator.comparing(SubOrder::getId))
                .map(sub -> SubOrderDetail.from(sub, latestAfterSales.get(sub.getId())))
                .toList();

        return new AdminOrderDetailVO(
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getRemark(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getUser().getNickname(),
                order.getUser().getPhone(),
                subOrders);
    }

    public record SubOrderDetail(
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
            String merchantRemark,
            List<OrderVO.OrderItemVO> items,
            AfterSaleBrief afterSale
    ) {

        static SubOrderDetail from(SubOrder subOrder, AfterSale afterSale) {
            List<OrderVO.OrderItemVO> items = subOrder.getItems().stream()
                    .sorted(Comparator.comparing(OrderItem::getId))
                    .map(OrderVO.OrderItemVO::from)
                    .toList();

            return new SubOrderDetail(
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
                    subOrder.getMerchantRemark(),
                    items,
                    afterSale == null ? null : AfterSaleBrief.from(afterSale));
        }
    }

    public record AfterSaleBrief(
            Long id,
            String afterSaleNo,
            String type,
            String status,
            BigDecimal amount,
            String reason,
            LocalDateTime createdAt,
            LocalDateTime handledAt
    ) {

        static AfterSaleBrief from(AfterSale afterSale) {
            return new AfterSaleBrief(
                    afterSale.getId(),
                    afterSale.getAfterSaleNo(),
                    afterSale.getType() == null ? "REFUND_ONLY" : afterSale.getType().name(),
                    afterSale.getStatus().name(),
                    afterSale.getAmount(),
                    afterSale.getReason(),
                    afterSale.getCreatedAt(),
                    afterSale.getHandledAt());
        }
    }
}
