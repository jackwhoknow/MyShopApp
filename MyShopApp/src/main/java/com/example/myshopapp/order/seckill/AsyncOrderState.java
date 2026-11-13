package com.example.myshopapp.order.seckill;

import java.math.BigDecimal;

/**
 * 排队请求在 Redis 中的状态快照，只允许提交者本人查询。
 */
public record AsyncOrderState(
        String status,
        Long orderId,
        BigDecimal totalAmount,
        String message,
        Long userId
) {
}
