package com.example.myshopapp.user.dto;

import com.example.myshopapp.user.entity.Role;
import com.example.myshopapp.user.entity.User;

import java.util.List;

/**
 * 对外暴露的用户信息，不包含密码等敏感字段。
 */
public record UserVO(Long id, String username, String nickname, String phone, List<String> roles) {

    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(),
                user.getRoles().stream().map(Role::getCode).sorted().toList());
    }
}
