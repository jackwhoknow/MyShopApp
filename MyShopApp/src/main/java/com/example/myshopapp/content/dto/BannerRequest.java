package com.example.myshopapp.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增/修改轮播入参。sortOrder、enabled 为空时服务层取默认值。
 */
public record BannerRequest(
        @NotBlank(message = "轮播标题不能为空")
        @Size(max = 100, message = "轮播标题不能超过 100 个字符")
        String title,

        @Size(max = 500, message = "图片地址不能超过 500 个字符")
        String imageUrl,

        @Size(max = 500, message = "跳转地址不能超过 500 个字符")
        String linkUrl,

        Integer sortOrder,

        Boolean enabled
) {
}
