package com.example.myshopapp.points;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.coupon.entity.Coupon;
import com.example.myshopapp.coupon.entity.UserCoupon;
import com.example.myshopapp.coupon.repository.CouponRepository;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import com.example.myshopapp.points.dto.PointCouponVO;
import com.example.myshopapp.points.entity.PointTransactionType;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分兑换优惠券：兑换一次即发放一张券，每人每张券只能兑换一次。
 */
@Service
@RequiredArgsConstructor
public class PointMallService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    @Transactional(readOnly = true)
    public List<PointCouponVO> available(Long userId) {
        return couponRepository.findPointsExchangeable(LocalDateTime.now(), ShopStatus.ACTIVE)
                .stream()
                .map(coupon -> PointCouponVO.from(coupon,
                        userCouponRepository.existsByCouponIdAndUserId(coupon.getId(), userId)))
                .toList();
    }

    @Transactional
    public PointCouponVO exchange(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在"));
        if (coupon.getPointsPrice() <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该券不支持积分兑换");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isEnabled()) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已停用");
        }
        if (now.isBefore(coupon.getStartAt()) || !now.isBefore(coupon.getEndAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券不在兑换时间内");
        }
        if (coupon.getClaimedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券已兑完");
        }
        if (coupon.getShop() != null && coupon.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "店铺暂停营业，暂不可兑换");
        }
        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已经兑换过这张券");
        }

        // 先扣积分再加券，积分不足会抛异常，整个事务回滚
        pointService.spend(userId, coupon.getPointsPrice(), PointTransactionType.POINT_EXCHANGE,
                "积分兑换优惠券：" + coupon.getTitle());
        coupon.setClaimedCount(coupon.getClaimedCount() + 1);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCoupon(coupon);
        userCoupon.setUser(userRepository.getReferenceById(userId));
        userCouponRepository.save(userCoupon);

        return PointCouponVO.from(coupon, true);
    }
}
