package com.example.myshopapp.payment.dto;

/**
 * 发起支付请求：channel 取值 ALIPAY / WECHAT，本地开发可用 MOCK。
 */
public record PaymentPrepareRequest(String channel) {
}
