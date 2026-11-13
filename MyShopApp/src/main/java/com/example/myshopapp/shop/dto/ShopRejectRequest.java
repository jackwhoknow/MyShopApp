package com.example.myshopapp.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 驳回店铺申请入参。
 */
public record ShopRejectRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 200, message = "驳回原因不能超过 200 个字符")
        String reason
) {
}
