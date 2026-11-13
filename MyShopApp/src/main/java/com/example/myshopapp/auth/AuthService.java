package com.example.myshopapp.auth;

import com.example.myshopapp.auth.dto.LoginRequest;
import com.example.myshopapp.auth.dto.LoginResponse;
import com.example.myshopapp.auth.dto.RegisterRequest;
import com.example.myshopapp.common.config.SecurityProperties;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.common.security.JwtUtil;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.common.security.TokenRevocationService;
import com.example.myshopapp.user.RoleCode;
import com.example.myshopapp.user.dto.UserVO;
import com.example.myshopapp.user.entity.Role;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.entity.UserStatus;
import com.example.myshopapp.user.repository.RoleRepository;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;
    private final TokenRevocationService tokenRevocationService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityProperties securityProperties;

    @Transactional
    public UserVO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        String roleCode = request.merchant() ? RoleCode.MERCHANT : RoleCode.BUYER;
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "角色未初始化"));

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname() : request.username());
        user.setPhone(request.phone());
        user.getRoles().add(role);
        userRepository.save(user);
        return UserVO.from(user);
    }

    /**
     * 登录：先做 IP 频率限制和图形验证码校验，再校验账号密码；
     * 连续失败达到阈值后锁定账号一段时间。
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        loginAttemptService.checkAndRecord(clientIp, securityProperties.loginRateLimit(),
                securityProperties.loginRateWindowSeconds());
        captchaService.verify(request.captchaId(), request.captchaCode());

        User user = userRepository.findByUsernameAndDeletedFalse(request.username())
                // 不区分“用户不存在”和“密码错误”，避免账号探测
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            long minutes = Math.max(1, java.time.Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            throw new BusinessException(ErrorCode.FORBIDDEN, "密码错误次数过多，账号已锁定，请 " + minutes + " 分钟后再试");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailure(user, now);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return issueTokens(user);
    }

    /**
     * 刷新登录态：refresh token 一次性使用，消费成功后签发新的 access + refresh token。
     */
    public LoginResponse refresh(String refreshToken) {
        Long userId = refreshTokenService.consume(refreshToken);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        return issueTokens(user);
    }

    /**
     * 退出登录：access token 加入黑名单立即失效，同时吊销本次使用的 refresh token。
     */
    public void logout(String accessToken, String refreshToken) {
        if (StringUtils.hasText(accessToken)) {
            try {
                JwtUtil.ParsedToken parsed = jwtUtil.parse(accessToken);
                tokenRevocationService.revokeToken(accessToken, parsed.expiresAt());
            } catch (Exception ignored) {
                // token 本身无效时无需处理
            }
        }
        refreshTokenService.revoke(refreshToken);
    }

    private LoginResponse issueTokens(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), roles);
        String accessToken = jwtUtil.generateToken(loginUser);
        String refreshToken = refreshTokenService.issue(user.getId());
        return new LoginResponse(accessToken, refreshToken,
                jwtUtil.getExpirationMs(), refreshTokenService.getExpirationMs(), UserVO.from(user));
    }

    private void registerFailure(User user, LocalDateTime now) {
        int failures = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(failures);
        if (failures >= securityProperties.maxLoginFailures()) {
            user.setLockedUntil(now.plusMinutes(securityProperties.lockMinutes()));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }
}
