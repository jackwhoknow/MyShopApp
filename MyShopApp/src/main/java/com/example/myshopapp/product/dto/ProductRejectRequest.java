package com.example.myshopapp.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员驳回商品入参。
 */
public record ProductRejectRequest(
        @NotBlank(message = "请填写驳回理由")
        @Size(max = 200, message = "驳回理由不能超过 200 个字符")
        String reason
) {
}
