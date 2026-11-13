package com.example.myshopapp.payment.dto;

/**
 * 支付状态轮询结果。
 */
public record PaymentStatusVO(Long orderId, String orderNo, String status, boolean paid) {
}
