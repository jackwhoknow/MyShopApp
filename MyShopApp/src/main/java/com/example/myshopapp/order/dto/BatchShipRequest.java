package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量发货入参：多个待发货子订单共用一组物流信息。
 */
public record BatchShipRequest(
        @NotEmpty(message = "请选择要发货的子订单")
        @Size(max = 50, message = "单次最多发货 50 个子订单")
        List<Long> subOrderIds,
        @Size(max = 50, message = "物流公司不能超过 50 个字符") String shipCompany,
        @Size(max = 50, message = "运单号不能超过 50 个字符") String trackingNo
) {
}
