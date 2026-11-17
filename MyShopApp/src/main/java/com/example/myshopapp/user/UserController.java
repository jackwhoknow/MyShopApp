package com.example.myshopapp.user;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.user.dto.AccountDeletionRequest;
import com.example.myshopapp.user.dto.PasswordChangeRequest;
import com.example.myshopapp.user.dto.ProfileUpdateRequest;
import com.example.myshopapp.user.dto.UserDataExportVO;
import com.example.myshopapp.user.dto.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDataService userDataService;

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

    /** 导出个人数据：资料、地址、订单、评价、优惠券与积分记录 */
    @GetMapping("/export")
    public ApiResponse<UserDataExportVO> exportData(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(userDataService.export(requireLogin(loginUser).id()));
    }

    /** 自助注销：验证密码后匿名化账号并吊销全部登录凭证 */
    @PostMapping("/deactivate")
    public ApiResponse<Void> deactivate(@AuthenticationPrincipal LoginUser loginUser,
                                        @Valid @RequestBody AccountDeletionRequest request) {
        userDataService.deactivate(requireLogin(loginUser).id(), request.password());
        return ApiResponse.ok();
    }

    private LoginUser requireLogin(LoginUser loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }
}
