package com.example.myshopapp.order.entity;

/**
 * 主订单状态：待支付 -> 已支付 -> 已完成，待支付可取消。
 */
public enum OrderStatus {

    PENDING_PAYMENT,
    PAID,
    COMPLETED,
    CANCELLED
}
