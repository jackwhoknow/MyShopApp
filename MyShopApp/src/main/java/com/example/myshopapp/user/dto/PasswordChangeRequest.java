package com.example.myshopapp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求：需要校验原密码。
 */
public record PasswordChangeRequest(
        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 50, message = "新密码长度需在 8-50 之间")
        @jakarta.validation.constraints.Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,50}$", message = "新密码必须同时包含字母和数字")
        String newPassword
) {
}
