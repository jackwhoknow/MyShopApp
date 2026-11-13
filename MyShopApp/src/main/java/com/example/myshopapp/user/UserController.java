package com.example.myshopapp.user;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.user.dto.PasswordChangeRequest;
import com.example.myshopapp.user.dto.ProfileUpdateRequest;
import com.example.myshopapp.user.dto.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(userService.getProfile(requireLogin(loginUser).id()));
    }

    @PutMapping("/me")
    public ApiResponse<UserVO> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                             @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(userService.updateProfile(requireLogin(loginUser).id(), request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(requireLogin(loginUser).id(), request);
        return ApiResponse.ok();
    }

    private LoginUser requireLogin(LoginUser loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }
}
