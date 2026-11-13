package com.example.myshopapp.payment.dto;

/**
 * 支付准备结果：支付宝返回跳转地址，微信返回二维码内容；paid=true 表示已直接完成（仅模拟支付）。
 */
public record PaymentPrepareVO(String channel, String payUrl, String codeUrl, boolean paid) {
}
