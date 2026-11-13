package com.example.myshopapp.order.entity;

/**
 * 支付记录状态：成功入账、校验失败，以及钱已到账但订单已关闭、需要退款的异常情况。
 */
public enum PaymentStatus {

    SUCCESS,
    FAILED,
    REFUND_REQUIRED,
    REFUNDED
}
