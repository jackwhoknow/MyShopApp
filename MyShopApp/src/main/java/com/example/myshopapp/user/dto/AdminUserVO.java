package com.example.myshopapp.user.dto;

import com.example.myshopapp.common.util.MaskUtil;
import com.example.myshopapp.user.entity.Role;
import com.example.myshopapp.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户管理列表项：包含状态与注册时间，不包含密码等敏感字段。
 */
public record AdminUserVO(
        Long id,
        String username,
        String nickname,
        String phone,
        List<String> roles,
        String status,
        LocalDateTime createdAt
) {

    public static AdminUserVO from(User user) {
        return new AdminUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                // 后台列表不展示完整手机号，降低数据泄露风险
                MaskUtil.maskPhone(user.getPhone()),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.getStatus().name(),
                user.getCreatedAt());
    }
}
