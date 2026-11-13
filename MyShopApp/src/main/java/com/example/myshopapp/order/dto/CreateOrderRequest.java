package com.example.myshopapp.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建订单：指定收货地址；cartItemIds 为空表示结算整个购物车。
 * userCouponIds 为本次使用的领券记录 id，同一店铺最多一张。
 */
public record CreateOrderRequest(
        @NotNull(message = "请选择收货地址") Long addressId,
        @Size(max = 200, message = "备注不能超过 200 字") String remark,
        List<Long> cartItemIds,
        List<Long> userCouponIds
) {
}
