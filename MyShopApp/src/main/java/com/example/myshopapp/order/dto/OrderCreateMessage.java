package com.example.myshopapp.order.dto;

import java.util.List;
import java.util.Map;

/**
 * 下单排队消息：提交阶段已经确定购物车项与 SKU 数量，消费端据此落库。
 * quantities 用于消费失败时精确回补 Redis 预扣的库存。
 */
public record OrderCreateMessage(
        String requestId,
        Long userId,
        Long addressId,
        String remark,
        List<Long> cartItemIds,
        List<Long> userCouponIds,
        Map<Long, Integer> quantities
) {

    public CreateOrderRequest toRequest() {
        return new CreateOrderRequest(addressId, remark, cartItemIds, userCouponIds);
    }
}
