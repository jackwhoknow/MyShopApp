package com.example.myshopapp.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增/修改分类入参。parentId 为 null 表示根分类。
 */
public record CategoryRequest(
        Long parentId,

        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称不能超过 50 个字符")
        String name,

        Integer sortOrder,

        Boolean enabled
) {
}
