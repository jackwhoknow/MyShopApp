package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.Size;

/**
 * 商家订单备注入参：传空字符串或空白表示清空备注。
 */
public record MerchantRemarkRequest(
        @Size(max = 200, message = "备注不能超过 200 个字符") String remark
) {
}
