package com.example.myshopapp.support.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 发起客服会话：记录来自哪个店铺，可选关联订单。
 */
public record StartConversationRequest(
        @NotNull(message = "请选择店铺") Long shopId,
        Long orderId
) {
}
