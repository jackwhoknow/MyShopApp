package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 秒杀下单请求：字段与 {@link CreateOrderRequest} 一致，
 * 额外携带客户端生成的 requestId 作为幂等键，重复提交不会生成多张订单。
 */
public record SeckillOrderRequest(
        @Size(max = 64, message = "请求编号过长") String requestId,
        @NotNull(message = "请选择收货地址") Long addressId,
        @Size(max = 200, message = "备注不能超过 200 字") String remark,
        List<Long> cartItemIds,
        List<Long> userCouponIds,
        Integer points
) {

    public CreateOrderRequest toCreateRequest() {
        return new CreateOrderRequest(addressId, remark, cartItemIds, userCouponIds, points);
    }
}
