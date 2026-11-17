package com.example.myshopapp.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改个人资料请求：仅允许修改昵称和手机号，用户名不可更改。
 */
public record ProfileUpdateRequest(
        @Size(max = 50, message = "昵称长度不能超过 50")
        String nickname,

        /** 传空字符串表示清空手机号 */
        @Size(max = 20, message = "手机号长度不能超过 20")
        String phone,

        /** 传空字符串表示清空邮箱；填写后可用于找回密码 */
        @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100")
        String email
) {
}
