package com.example.myshopapp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 邮箱验证码重置密码。
 */
public record PasswordResetRequest(
        @NotBlank(message = "请输入邮箱")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100")
        String email,

        @NotBlank(message = "请输入验证码")
        @Size(max = 10, message = "验证码格式不正确")
        String code,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 30, message = "密码长度需在 8-30 之间")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,30}$", message = "密码必须同时包含字母和数字")
        String newPassword
) {
}
