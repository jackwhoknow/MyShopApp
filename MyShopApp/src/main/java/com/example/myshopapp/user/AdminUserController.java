package com.example.myshopapp.user;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.user.dto.AdminUserVO;
import com.example.myshopapp.user.dto.UserStatusUpdateRequest;
import com.example.myshopapp.user.entity.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端用户管理接口，需要 ADMIN 角色（由 SecurityConfig 统一拦截）。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<AdminUserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(userService.adminList(keyword, role, status, page, size));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserVO> updateStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.ok(userService.updateStatus(loginUser.id(), id, request.status()));
    }
}
