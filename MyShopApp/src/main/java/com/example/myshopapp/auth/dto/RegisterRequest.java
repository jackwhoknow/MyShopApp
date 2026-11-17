package com.example.myshopapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 30, message = "长度需在 3-30 之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 30, message = "密码长度需在 8-30 之间")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,30}$", message = "密码必须同时包含字母和数字")
        String password,

        @Size(max = 50, message = "长度不能超过 50")
        String nickname,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        /** 邮箱可选，填写后可用于找回密码 */
        @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100")
        String email,

        /** true = 商家注册，false = 普通买家 */
        boolean merchant) {
}
