package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.Size;

/**
 * 商家处理售后：同意退款或驳回，可附带备注。
 */
public record AfterSaleHandleRequest(
        @Size(max = 200, message = "处理备注不能超过 200 个字符")
        String merchantRemark
) {
}
