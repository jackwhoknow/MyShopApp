package com.example.myshopapp.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增/修改公告入参。pinned、enabled 为空时服务层取默认值。
 */
public record AnnouncementRequest(
        @NotBlank(message = "公告标题不能为空")
        @Size(max = 100, message = "公告标题不能超过 100 个字符")
        String title,

        @NotBlank(message = "公告内容不能为空")
        @Size(max = 2000, message = "公告内容不能超过 2000 个字符")
        String content,

        Boolean pinned,

        Boolean enabled
) {
}
