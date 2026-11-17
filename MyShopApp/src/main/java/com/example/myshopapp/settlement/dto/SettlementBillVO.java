package com.example.myshopapp.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算账单明细：一条记录对应一个子订单，settleable 表示该笔金额是否计入可结算收入。
 */
public record SettlementBillVO(
        Long subOrderId,
        String subOrderNo,
        String orderNo,
        BigDecimal amount,
        String status,
        LocalDateTime paidAt,
        LocalDateTime completedAt,
        boolean settleable
) {
}
