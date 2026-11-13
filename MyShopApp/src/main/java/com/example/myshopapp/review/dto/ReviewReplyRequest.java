package com.example.myshopapp.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商家回复评价入参。
 */
public record ReviewReplyRequest(
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 500, message = "回复内容不能超过 500 个字符")
        String reply
) {
}
