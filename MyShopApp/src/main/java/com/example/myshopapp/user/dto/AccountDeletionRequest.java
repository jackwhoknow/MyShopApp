package com.example.myshopapp.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 账号注销入参：必须验证当前密码，防止 token 被盗后恶意注销。
 */
public record AccountDeletionRequest(@NotBlank(message = "请输入登录密码") String password) {
}
