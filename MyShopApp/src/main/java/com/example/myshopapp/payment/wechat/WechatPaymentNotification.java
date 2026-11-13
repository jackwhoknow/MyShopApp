package com.example.myshopapp.payment.wechat;

import java.math.BigDecimal;

/**
 * 微信支付回调解密后的关键信息。
 */
public record WechatPaymentNotification(
        String outTradeNo,
        String transactionId,
        String tradeState,
        int totalFen,
        String appId,
        String mchId) {

    public BigDecimal totalAmount() {
        return BigDecimal.valueOf(totalFen).movePointLeft(2);
    }
}
