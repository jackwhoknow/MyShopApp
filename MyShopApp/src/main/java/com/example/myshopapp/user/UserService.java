package com.example.myshopapp.user;

import com.example.myshopapp.auth.RefreshTokenService;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.common.security.TokenRevocationService;
import com.example.myshopapp.user.dto.AdminUserVO;
import com.example.myshopapp.user.dto.PasswordChangeRequest;
import com.example.myshopapp.user.dto.ProfileUpdateRequest;
import com.example.myshopapp.user.dto.UserVO;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.entity.UserStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocationService tokenRevocationService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserVO getProfile(Long userId) {
        return UserVO.from(loadActiveUser(userId));
    }

    /**
     * 修改昵称和手机号：昵称为空时保持原值，手机号传空字符串表示清空。
     */
    @Transactional
    public UserVO updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = loadActiveUser(userId);
        if (StringUtils.hasText(request.nickname())) {
            user.setNickname(request.nickname().trim());
        }
        if (request.phone() != null) {
            user.setPhone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null);
        }
        userRepository.save(user);
        return UserVO.from(user);
    }

    /**
     * 修改密码：原密码不正确时拒绝修改。
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = loadActiveUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        // 改密后旧 token 与 refresh token 全部失效，防止密码泄露后攻击者继续续期
        tokenRevocationService.revokeAllForUser(userId);
        refreshTokenService.revokeAll(userId);
    }

    /** 后台用户列表：关键词、角色、状态均可选，按注册 id 倒序 */
    @Transactional(readOnly = true)
    public PageResult<AdminUserVO> adminList(String keyword, String role, UserStatus status,
                                             int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        String roleFilter = StringUtils.hasText(role) ? role.trim() : "";
        Page<User> users = userRepository.searchAdmin(kw, roleFilter, status, pageable);
        return PageResult.of(users.map(AdminUserVO::from));
    }

    /** 管理员启用/禁用用户，禁止禁用当前登录的管理员自己 */
    @Transactional
    public AdminUserVO updateStatus(Long operatorId, Long userId, UserStatus status) {
        if (operatorId.equals(userId) && status == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能禁用当前登录的管理员账号");
        }
        User user = loadActiveUser(userId);
        user.setStatus(status);
        userRepository.save(user);
        if (status == UserStatus.DISABLED) {
            // 禁用后立即吊销该用户已签发的 token 与 refresh token
            tokenRevocationService.revokeAllForUser(userId);
            refreshTokenService.revokeAll(userId);
        }
        return AdminUserVO.from(user);
    }

    private User loadActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }
}
