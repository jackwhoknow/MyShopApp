package com.example.myshopapp.notification.dto;

import com.example.myshopapp.notification.entity.NotificationAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理员群发通知入参。
 */
public record AdminBroadcastRequest(
        @NotBlank(message = "请填写通知标题")
        @Size(max = 100, message = "标题不能超过 100 个字符")
        String title,

        @NotBlank(message = "请填写通知内容")
        @Size(max = 500, message = "内容不能超过 500 个字符")
        String content,

        @NotNull(message = "请选择发送范围")
        NotificationAudience audience,

        @Size(max = 200, message = "跳转地址不能超过 200 个字符")
        String link
) {
}
