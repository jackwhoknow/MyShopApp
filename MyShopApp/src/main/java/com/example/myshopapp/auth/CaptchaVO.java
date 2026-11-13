package com.example.myshopapp.auth;

/**
 * 图形验证码响应：captchaId 随登录请求一起提交，image 为可直接展示的 PNG Data URL。
 */
public record CaptchaVO(String captchaId, String image) {
}
