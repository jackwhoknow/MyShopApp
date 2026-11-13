package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 买家填写退货物流：仅退货退款且商家已同意退货时可提交。
 */
public record ReturnShipmentRequest(
        @NotBlank(message = "退货物流公司不能为空")
        @Size(max = 50, message = "退货物流公司不能超过 50 个字符") String returnCompany,
        @NotBlank(message = "退货运单号不能为空")
        @Size(max = 50, message = "退货运单号不能超过 50 个字符") String returnTrackingNo) {
}
