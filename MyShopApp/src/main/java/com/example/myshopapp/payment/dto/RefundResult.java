package com.example.myshopapp.payment.dto;

/**
 * 退款结果：success=false 时 message 为渠道返回的失败原因。
 */
public record RefundResult(boolean success, String refundNo, String message) {

    public static RefundResult ok(String refundNo) {
        return new RefundResult(true, refundNo, null);
    }

    public static RefundResult fail(String message) {
        return new RefundResult(false, null, message);
    }
}
