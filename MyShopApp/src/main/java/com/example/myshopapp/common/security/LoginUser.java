package com.example.myshopapp.common.security;

import java.util.Set;

/**
 * 登录用户：从 JWT 解析出来，作为 SecurityContext 的 principal。
 */
public record LoginUser(Long id, String username, Set<String> roles) {
}
