package com.example.myshopapp.auth.dto;

import com.example.myshopapp.user.dto.UserVO;

/**
 * 登录响应：token 为短期 access token，refreshToken 用于续期（一次性轮换）。
 * 有效期单位均为毫秒。
 */
public record LoginResponse(
        String token,
        String refreshToken,
        long expiresIn,
        long refreshExpiresIn,
        UserVO user
) {
}
