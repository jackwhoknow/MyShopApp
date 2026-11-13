package com.example.myshopapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 刷新登录态 / 退出登录时携带的 refresh token。
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken 不能为空")
        @Size(max = 128, message = "refreshToken 格式不正确")
        String refreshToken
) {
}
