package com.example.myshopapp.seckill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家创建秒杀活动：活动价必须低于 SKU 原价，活动时间不能与同 SKU 的其他活动重叠。
 */
public record SeckillActivityCreateRequest(
        @NotNull(message = "请选择秒杀商品规格") Long skuId,
        @NotNull(message = "请填写活动价") @DecimalMin(value = "0.01", message = "活动价需大于 0") BigDecimal activityPrice,
        @NotNull(message = "请填写活动库存") @Min(value = 1, message = "活动库存至少为 1") Integer totalStock,
        @NotNull(message = "请填写每人限购") @Min(value = 1, message = "每人限购至少为 1") Integer perUserLimit,
        @NotNull(message = "请选择开始时间") LocalDateTime startAt,
        @NotNull(message = "请选择结束时间") LocalDateTime endAt
) {
}
