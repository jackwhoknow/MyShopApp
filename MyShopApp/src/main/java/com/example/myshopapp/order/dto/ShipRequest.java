package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商家发货入参：必须提供真实的物流公司与运单号，系统不生成任何模拟单号。
 */
public record ShipRequest(
        @NotBlank(message = "请填写物流公司") @Size(max = 50, message = "物流公司不能超过 50 个字符") String shipCompany,
        @NotBlank(message = "请填写真实运单号") @Size(max = 50, message = "运单号不能超过 50 个字符") String trackingNo
) {
}
