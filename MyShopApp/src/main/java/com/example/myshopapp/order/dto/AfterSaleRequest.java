package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 买家申请退款：按子订单发起，可上传凭证图片。
 */
public record AfterSaleRequest(
        @NotNull(message = "必须指定子订单")
        Long subOrderId,

        @NotBlank(message = "退款原因不能为空")
        @Size(max = 200, message = "退款原因不能超过 200 个字符")
        String reason,

        @Size(max = 6, message = "最多上传 6 张凭证图片")
        List<String> evidenceImages
) {
}
