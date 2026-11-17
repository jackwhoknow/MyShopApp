package com.example.myshopapp.settlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 商家发起提现：金额从可提现余额中冻结，收款信息必填。
 */
public record WithdrawalCreateRequest(
        @NotNull(message = "请填写提现金额")
        @DecimalMin(value = "0.01", message = "提现金额需大于 0")
        BigDecimal amount,
        @NotBlank(message = "请填写收款户名") @Size(max = 50, message = "收款户名不能超过 50 字")
        String accountName,
        @NotBlank(message = "请填写收款账号") @Size(max = 64, message = "收款账号不能超过 64 字")
        String accountNo,
        @NotBlank(message = "请填写开户银行") @Size(max = 100, message = "开户银行不能超过 100 字")
        String bankName
) {
}
