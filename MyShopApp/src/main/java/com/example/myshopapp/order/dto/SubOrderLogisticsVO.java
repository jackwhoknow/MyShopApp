package com.example.myshopapp.order.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 子订单物流轨迹：具体轨迹由物流 Provider 生成，本对象只承载对外展示字段。
 */
public record SubOrderLogisticsVO(
        Long subOrderId,
        String subOrderNo,
        String status,
        String shipCompany,
        String trackingNo,
        LocalDateTime shippedAt,
        LocalDateTime completedAt,
        List<TraceVO> traces
) {

    public record TraceVO(
            String title,
            String description,
            String location,
            LocalDateTime occurredAt
    ) {
    }
}
