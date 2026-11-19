package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改物流信息入参：允许只修改其中一个字段，但不能都为空。
 * 与发货入参分开，避免必填校验破坏局部更新语义。
 */
public record UpdateLogisticsRequest(
        @Size(max = 50, message = "物流公司不能超过 50 个字符") String shipCompany,
        @Size(max = 50, message = "运单号不能超过 50 个字符") String trackingNo
) {
}
