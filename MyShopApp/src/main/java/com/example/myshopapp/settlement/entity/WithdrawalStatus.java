package com.example.myshopapp.settlement.entity;

/**
 * 提现申请状态：商家提交 -> 平台审核 -> 打款完成。
 */
public enum WithdrawalStatus {

    /** 商家已提交，等待平台审核 */
    PENDING,
    /** 平台审核通过，等待打款 */
    APPROVED,
    /** 平台驳回，金额退回可提现余额 */
    REJECTED,
    /** 平台已打款 */
    PAID
}
