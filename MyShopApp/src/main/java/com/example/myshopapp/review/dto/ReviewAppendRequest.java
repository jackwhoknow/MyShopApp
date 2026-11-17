package com.example.myshopapp.review.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 追评入参：内容与图片至少填写一项，每笔评价只能追评一次。
 */
public record ReviewAppendRequest(
        @Size(max = 500, message = "追评内容不能超过 500 个字符")
        String content,

        @Size(max = 6, message = "最多上传 6 张图片")
        List<String> images
) {
}
