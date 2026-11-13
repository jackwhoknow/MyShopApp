package com.example.myshopapp.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 开店申请入参。
 */
public record ShopApplyRequest(
        @NotBlank(message = "店铺名称不能为空")
        @Size(max = 50, message = "店铺名称不能超过 50 个字符")
        String name,

        @Size(max = 200, message = "店铺简介不能超过 200 个字符")
        String description
) {
}
