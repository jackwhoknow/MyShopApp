package com.example.myshopapp.settlement.dto;

import java.math.BigDecimal;

/**
 * 商家结算概览：可结算收入、退款、已提现、冻结中与可提现余额。
 */
public record SettlementSummaryVO(
        BigDecimal totalIncome,
        BigDecimal refundedAmount,
        BigDecimal withdrawnAmount,
        BigDecimal frozenAmount,
        BigDecimal availableAmount,
        long pendingWithdrawalCount,
        long totalWithdrawalCount
) {
}
