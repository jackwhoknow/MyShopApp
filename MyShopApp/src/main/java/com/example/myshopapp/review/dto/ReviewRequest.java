package com.example.myshopapp.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 买家评价入参：针对订单中的某件商品评分并填写评价内容。
 */
public record ReviewRequest(
        @NotNull(message = "必须指定订单商品")
        Long orderItemId,

        @NotNull(message = "请选择评分")
        @Min(value = 1, message = "评分最低 1 分")
        @Max(value = 5, message = "评分最高 5 分")
        Integer rating,

        @Size(max = 500, message = "评价内容不能超过 500 个字符")
        String content,

        @Size(max = 6, message = "最多上传 6 张图片")
        List<String> images
) {
}
