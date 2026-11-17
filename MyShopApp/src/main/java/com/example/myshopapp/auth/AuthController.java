package com.example.myshopapp.auth;

import com.example.myshopapp.auth.dto.EmailCodeRequest;
import com.example.myshopapp.auth.dto.LoginRequest;
import com.example.myshopapp.auth.dto.LoginResponse;
import com.example.myshopapp.auth.dto.PasswordResetRequest;
import com.example.myshopapp.auth.dto.RefreshTokenRequest;
import com.example.myshopapp.auth.dto.RegisterRequest;
import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.user.dto.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final EmailCodeService emailCodeService;

    @GetMapping("/captcha")
    public ApiResponse<CaptchaVO> captcha() {
        return ApiResponse.ok(captchaService.create());
    }

    /** 忘记密码：向注册邮箱发送验证码（本地环境验证码打印在应用日志） */
    @PostMapping("/password/code")
    public ApiResponse<Void> sendResetCode(@Valid @RequestBody EmailCodeRequest request) {
        emailCodeService.sendResetCode(request.email());
        return ApiResponse.ok();
    }

    /** 忘记密码：邮箱验证码重置密码 */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request, clientIp(servletRequest)));
    }

    /** 刷新登录态：refresh token 一次性使用并轮换 */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody(required = false) RefreshTokenRequest request) {
        String accessToken = StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : null;
        authService.logout(accessToken, request == null ? null : request.refreshToken());
        return ApiResponse.ok();
    }

    /**
     * 取真实客户端 IP：优先使用反向代理透传的 X-Forwarded-For。
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
