package com.example.myshopapp.order.dto;

import com.example.myshopapp.order.entity.AfterSale;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 售后单视图：同时服务买家“我的退款”和商家“售后处理”。
 */
public record AfterSaleVO(
        Long id,
        String afterSaleNo,
        Long subOrderId,
        String subOrderNo,
        Long orderId,
        String orderNo,
        Long shopId,
        String shopName,
        String buyerUsername,
        BigDecimal amount,
        String reason,
        List<String> evidenceImages,
        String type,
        String status,
        String merchantRemark,
        String adminRemark,
        String returnCompany,
        String returnTrackingNo,
        LocalDateTime returnedAt,
        LocalDateTime receivedAt,
        LocalDateTime createdAt,
        LocalDateTime handledAt,
        List<OrderVO.OrderItemVO> items
) {

    public static AfterSaleVO from(AfterSale afterSale) {
        SubOrder subOrder = afterSale.getSubOrder();
        List<OrderVO.OrderItemVO> items = subOrder.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .map(OrderVO.OrderItemVO::from)
                .toList();

        return new AfterSaleVO(
                afterSale.getId(),
                afterSale.getAfterSaleNo(),
                subOrder.getId(),
                subOrder.getSubOrderNo(),
                subOrder.getOrder().getId(),
                subOrder.getOrder().getOrderNo(),
                afterSale.getShopId(),
                subOrder.getShop().getName(),
                subOrder.getOrder().getUser().getUsername(),
                afterSale.getAmount(),
                afterSale.getReason(),
                afterSale.getEvidenceImages() == null || afterSale.getEvidenceImages().isBlank()
                        ? List.of()
                        : java.util.Arrays.stream(afterSale.getEvidenceImages().split(","))
                                .filter(value -> !value.isBlank()).toList(),
                afterSale.getType() == null ? "REFUND_ONLY" : afterSale.getType().name(),
                afterSale.getStatus().name(),
                afterSale.getMerchantRemark(),
                afterSale.getAdminRemark(),
                afterSale.getReturnCompany(),
                afterSale.getReturnTrackingNo(),
                afterSale.getReturnedAt(),
                afterSale.getReceivedAt(),
                afterSale.getCreatedAt(),
                afterSale.getHandledAt(),
                items);
    }
}
