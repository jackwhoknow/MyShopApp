package com.example.myshopapp.settlement.dto;

import java.math.BigDecimal;

/**
 * 平台提现审核概览：待审核、待打款与已打款金额。
 */
public record AdminSettlementSummaryVO(
        long pendingCount,
        BigDecimal pendingAmount,
        long approvedCount,
        BigDecimal approvedAmount,
        long paidCount,
        BigDecimal paidAmount
) {
}
