package com.example.myshopapp.order.dto;

import java.util.List;

/**
 * 批量发货结果：成功数量与失败明细，失败的订单不影响其余订单发货。
 */
public record BatchShipResultVO(
        int total,
        int shipped,
        List<Failure> failures
) {

    public record Failure(Long subOrderId, String subOrderNo, String reason) {
    }
}
