package com.example.myshopapp.user;

import com.example.myshopapp.address.entity.UserAddress;
import com.example.myshopapp.address.repository.UserAddressRepository;
import com.example.myshopapp.auth.RefreshTokenService;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.common.security.TokenRevocationService;
import com.example.myshopapp.coupon.entity.UserCoupon;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.points.entity.PointAccount;
import com.example.myshopapp.points.repository.PointAccountRepository;
import com.example.myshopapp.points.repository.PointTransactionRepository;
import com.example.myshopapp.review.entity.Review;
import com.example.myshopapp.review.repository.ReviewRepository;
import com.example.myshopapp.user.dto.UserDataExportVO;
import com.example.myshopapp.user.dto.UserVO;
import com.example.myshopapp.user.entity.Role;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.entity.UserStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 账号生命周期：个人数据导出与自助注销。
 * 注销后保留脱敏的用户 ID 以维持订单等历史记录的关联，身份信息全部清除。
 */
@Service
@RequiredArgsConstructor
public class UserDataService {

    /** 导出上限，避免超大账号一次拉取过多数据 */
    private static final int EXPORT_LIMIT = 500;
    private static final int POINT_LIMIT = 200;

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointAccountRepository pointAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocationService tokenRevocationService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserDataExportVO export(Long userId) {
        User user = loadActiveUser(userId);
        List<UserDataExportVO.AddressItem> addresses = addressRepository
                .findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
                .map(UserDataService::toAddressItem)
                .toList();
        List<UserDataExportVO.OrderItem> orders = orderRepository
                .findByUserIdOrderByIdDesc(userId, PageRequest.of(0, EXPORT_LIMIT)).stream()
                .map(UserDataService::toOrderItem)
                .toList();
        List<UserDataExportVO.ReviewItem> reviews = reviewRepository
                .findByUserIdOrderByIdDesc(userId, PageRequest.of(0, EXPORT_LIMIT)).stream()
                .map(UserDataService::toReviewItem)
                .toList();
        List<UserDataExportVO.CouponItem> coupons = userCouponRepository.findMine(userId).stream()
                .map(UserDataService::toCouponItem)
                .toList();

        return new UserDataExportVO(LocalDateTime.now(), UserVO.from(user),
                addresses, orders, reviews, coupons, points(userId));
    }

    /**
     * 自助注销：校验密码后匿名化账号并吊销全部登录凭证。
     * 商家与管理员账号涉及资金与平台职责，必须联系平台处理。
     */
    @Transactional
    public void deactivate(Long userId, String password) {
        User user = loadActiveUser(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "登录密码不正确");
        }
        boolean privileged = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(code -> RoleCode.ADMIN.equals(code) || RoleCode.MERCHANT.equals(code));
        if (privileged) {
            throw new BusinessException(ErrorCode.CONFLICT, "商家或管理员账号不支持自助注销，请联系平台");
        }

        user.setUsername("deleted_" + userId);
        user.setNickname("已注销用户");
        user.setPhone(null);
        user.setEmail(null);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.DISABLED);
        user.setDeleted(true);
        userRepository.save(user);

        tokenRevocationService.revokeAllForUser(userId);
        refreshTokenService.revokeAll(userId);
    }

    private UserDataExportVO.PointsSummary points(Long userId) {
        PointAccount account = pointAccountRepository.findByUserId(userId).orElse(null);
        List<UserDataExportVO.PointItem> transactions = pointTransactionRepository
                .findByUserIdOrderByIdDesc(userId, PageRequest.of(0, POINT_LIMIT)).stream()
                .map(item -> new UserDataExportVO.PointItem(
                        item.getChangeAmount(), item.getBalanceAfter(), item.getType().name(),
                        item.getOrderNo(), item.getRemark(), item.getCreatedAt()))
                .toList();
        if (account == null) {
            return new UserDataExportVO.PointsSummary(0, 0, 0, transactions);
        }
        return new UserDataExportVO.PointsSummary(account.getBalance(), account.getTotalEarned(),
                account.getTotalSpent(), transactions);
    }

    private static UserDataExportVO.AddressItem toAddressItem(UserAddress address) {
        return new UserDataExportVO.AddressItem(address.getReceiverName(), address.getPhone(),
                address.getProvince(), address.getCity(), address.getDistrict(), address.getDetail(),
                address.isDefaultAddress());
    }

    private static UserDataExportVO.OrderItem toOrderItem(Order order) {
        return new UserDataExportVO.OrderItem(order.getOrderNo(), order.getTotalAmount(),
                order.getStatus().name(), order.getPointsUsed(), order.getPointsEarned(),
                order.getCreatedAt(), order.getPaidAt());
    }

    private static UserDataExportVO.ReviewItem toReviewItem(Review review) {
        return new UserDataExportVO.ReviewItem(review.getId(), review.getProductId(), review.getRating(),
                review.getContent(), review.getImages(), review.getStatus().name(), review.getCreatedAt());
    }

    private static UserDataExportVO.CouponItem toCouponItem(UserCoupon userCoupon) {
        return new UserDataExportVO.CouponItem(userCoupon.getCoupon().getTitle(),
                userCoupon.getCoupon().getThresholdAmount(), userCoupon.getCoupon().getDiscountAmount(),
                userCoupon.getStatus().name(), userCoupon.getClaimedAt(), userCoupon.getUsedAt(),
                userCoupon.getCoupon().getEndAt());
    }

    private User loadActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }
}
