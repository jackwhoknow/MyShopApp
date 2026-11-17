package com.example.myshopapp.settlement.dto;

import com.example.myshopapp.settlement.entity.WithdrawalRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现单视图，商家端与平台端共用。
 */
public record WithdrawalVO(
        Long id,
        String withdrawalNo,
        Long shopId,
        String shopName,
        BigDecimal amount,
        String accountName,
        String accountNo,
        String bankName,
        String status,
        String adminRemark,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime paidAt
) {

    public static WithdrawalVO from(WithdrawalRequest request) {
        return new WithdrawalVO(
                request.getId(),
                request.getWithdrawalNo(),
                request.getShop().getId(),
                request.getShop().getName(),
                request.getAmount(),
                request.getAccountName(),
                request.getAccountNo(),
                request.getBankName(),
                request.getStatus().name(),
                request.getAdminRemark(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getPaidAt());
    }
}
