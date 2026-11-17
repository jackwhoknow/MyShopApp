package com.example.myshopapp.points.dto;

import com.example.myshopapp.points.entity.PointTransaction;

import java.time.LocalDateTime;

/**
 * 积分流水视图。
 */
public record PointTransactionVO(
        Long id,
        int changeAmount,
        int balanceAfter,
        String type,
        Long orderId,
        String orderNo,
        String remark,
        LocalDateTime createdAt
) {

    public static PointTransactionVO from(PointTransaction transaction) {
        return new PointTransactionVO(
                transaction.getId(),
                transaction.getChangeAmount(),
                transaction.getBalanceAfter(),
                transaction.getType().name(),
                transaction.getOrderId(),
                transaction.getOrderNo(),
                transaction.getRemark(),
                transaction.getCreatedAt());
    }
}
