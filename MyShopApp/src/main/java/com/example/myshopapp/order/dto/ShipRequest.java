package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.Size;

/**
 * 商家发货入参：物流信息允许留空，方便演示环境直接发货。
 */
public record ShipRequest(
        @Size(max = 50, message = "物流公司不能超过 50 个字符") String shipCompany,
        @Size(max = 50, message = "运单号不能超过 50 个字符") String trackingNo
) {
}
