package com.example.myshopapp.coupon;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.coupon.dto.CouponVO;
import com.example.myshopapp.coupon.dto.UserCouponVO;
import com.example.myshopapp.coupon.entity.Coupon;
import com.example.myshopapp.coupon.entity.UserCoupon;
import com.example.myshopapp.coupon.repository.CouponRepository;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 买家领券：领券中心、领券、我的券。
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CouponVO> available(Long userId) {
        return couponRepository.findClaimable(userId, LocalDateTime.now(), ShopStatus.ACTIVE)
                .stream()
                .map(CouponVO::from)
                .toList();
    }

    /**
     * 领券：加锁读取券模板，依次校验启停、有效期、余量和重复领取。
     */
    @Transactional
    public UserCouponVO claim(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在"));

        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isEnabled()) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已停用");
        }
        if (now.isBefore(coupon.getStartAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券还未开始发放");
        }
        if (!now.isBefore(coupon.getEndAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已过期");
        }
        if (coupon.getClaimedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已领完");
        }
        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已经领取过这张券");
        }

        coupon.setClaimedCount(coupon.getClaimedCount() + 1);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCoupon(coupon);
        userCoupon.setUser(userRepository.getReferenceById(userId));
        userCouponRepository.save(userCoupon);

        return UserCouponVO.from(userCoupon, now);
    }

    /** 我的券：state 为空表示全部，支持 UNUSED / USED / EXPIRED 过滤 */
    @Transactional(readOnly = true)
    public List<UserCouponVO> mine(Long userId, String state) {
        LocalDateTime now = LocalDateTime.now();
        return userCouponRepository.findMine(userId).stream()
                .map(userCoupon -> UserCouponVO.from(userCoupon, now))
                .filter(vo -> state == null || state.isBlank() || vo.state().equals(state))
                .toList();
    }
}
