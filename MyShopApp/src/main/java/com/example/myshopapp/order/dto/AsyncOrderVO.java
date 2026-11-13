package com.example.myshopapp.order.dto;

import java.math.BigDecimal;

/**
 * 异步下单结果：提交后立即返回 PENDING，前端凭 requestId 轮询。
 * SUCCESS 时带回订单 id 与金额，FAILED 时 message 为可直接展示的原因。
 */
public record AsyncOrderVO(
        String requestId,
        String status,
        Long orderId,
        BigDecimal totalAmount,
        String message
) {

    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    public static AsyncOrderVO pending(String requestId) {
        return new AsyncOrderVO(requestId, PENDING, null, null, "已进入下单队列，正在为您锁定库存");
    }

    public static AsyncOrderVO success(String requestId, Long orderId, BigDecimal totalAmount) {
        return new AsyncOrderVO(requestId, SUCCESS, orderId, totalAmount, "下单成功");
    }

    public static AsyncOrderVO failed(String requestId, String message) {
        return new AsyncOrderVO(requestId, FAILED, null, null, message);
    }
}
