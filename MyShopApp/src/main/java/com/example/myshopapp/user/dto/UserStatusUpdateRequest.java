package com.example.myshopapp.user.dto;

import com.example.myshopapp.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 管理员启用/禁用用户请求。
 */
public record UserStatusUpdateRequest(
        @NotNull(message = "状态不能为空")
        UserStatus status
) {
}
