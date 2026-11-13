package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 平台介入售后：退款或关闭都必须填写介入说明，便于买卖双方了解处理依据。
 */
public record AdminAfterSaleActionRequest(
        @NotBlank(message = "请填写平台介入说明")
        @Size(max = 200, message = "平台介入说明不能超过 200 个字符")
        String remark
) {
}
