package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量发货入参：多个子订单共用一个真实运单号（同一包裹），不再篡改单号加后缀。
 */
public record BatchShipRequest(
        @NotEmpty(message = "请选择要发货的子订单")
        @Size(max = 50, message = "单次最多发货 50 个子订单")
        List<Long> subOrderIds,
        @NotBlank(message = "请填写物流公司") @Size(max = 50, message = "物流公司不能超过 50 个字符") String shipCompany,
        @NotBlank(message = "请填写真实运单号") @Size(max = 50, message = "运单号不能超过 50 个字符") String trackingNo
) {
}
