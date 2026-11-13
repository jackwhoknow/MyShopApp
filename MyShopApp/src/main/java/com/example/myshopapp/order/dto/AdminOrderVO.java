package com.example.myshopapp.order.dto;

import com.example.myshopapp.order.entity.AfterSale;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.SubOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 平台端订单列表视图：主订单 + 买家信息 + 按店铺拆分的子订单摘要。
 * 详情请见 {@link AdminOrderDetailVO}。
 */
public record AdminOrderVO(
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
        List<SubOrderBrief> subOrders
) {

    public static AdminOrderVO from(Order order, Map<Long, AfterSale> latestAfterSales) {
        List<SubOrderBrief> subOrders = order.getSubOrders().stream()
                .sorted(Comparator.comparing(SubOrder::getId))
                .map(sub -> SubOrderBrief.from(sub, latestAfterSales.get(sub.getId())))
                .toList();

        return new AdminOrderVO(
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
                subOrders);
    }

    public record SubOrderBrief(
            Long id,
            String subOrderNo,
            Long shopId,
            String shopName,
            String status,
            BigDecimal totalAmount,
            int itemCount,
            LocalDateTime createdAt,
            String shipCompany,
            String trackingNo,
            LocalDateTime shippedAt,
            String afterSaleStatus
    ) {

        static SubOrderBrief from(SubOrder subOrder, AfterSale afterSale) {
            int itemCount = subOrder.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
            return new SubOrderBrief(
                    subOrder.getId(),
                    subOrder.getSubOrderNo(),
                    subOrder.getShop().getId(),
                    subOrder.getShop().getName(),
                    subOrder.getStatus().name(),
                    subOrder.getTotalAmount(),
                    itemCount,
                    subOrder.getCreatedAt(),
                    subOrder.getShipCompany(),
                    subOrder.getTrackingNo(),
                    subOrder.getShippedAt(),
                    afterSale == null ? null : afterSale.getStatus().name());
        }
    }
}
