package com.example.myshopapp.seckill.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 秒杀活动启停请求。
 */
public record SeckillStatusRequest(
        @NotNull(message = "请选择启用状态") Boolean enabled
) {
}
