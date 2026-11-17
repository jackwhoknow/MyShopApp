package com.example.myshopapp.settlement.dto;

import jakarta.validation.constraints.Size;

/**
 * 平台审核提现：驳回时备注必填，由服务层校验。
 */
public record WithdrawalActionRequest(
        @Size(max = 200, message = "备注不能超过 200 字") String remark
) {
}
