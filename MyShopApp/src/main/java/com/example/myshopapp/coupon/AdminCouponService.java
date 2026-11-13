package com.example.myshopapp.coupon;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.coupon.dto.CouponCreateRequest;
import com.example.myshopapp.coupon.dto.MerchantCouponVO;
import com.example.myshopapp.coupon.entity.Coupon;
import com.example.myshopapp.coupon.entity.UserCouponStatus;
import com.example.myshopapp.coupon.repository.CouponRepository;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台券管理：管理员创建平台通用券、查看领取/使用情况、启用停用。
 */
@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional(readOnly = true)
    public List<MerchantCouponVO> listPlatform() {
        List<Coupon> coupons = couponRepository.findByShopIsNullOrderByIdDesc();
        if (coupons.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> usedCounts = userCouponRepository
                .countUsedByCouponIds(coupons.stream().map(Coupon::getId).toList(), UserCouponStatus.USED)
                .stream()
                .collect(Collectors.toMap(UserCouponRepository.CouponUsedCount::getCouponId,
                        UserCouponRepository.CouponUsedCount::getTotal));

        LocalDateTime now = LocalDateTime.now();
        return coupons.stream()
                .map(coupon -> MerchantCouponVO.from(coupon,
                        usedCounts.getOrDefault(coupon.getId(), 0L), now))
                .toList();
    }

    @Transactional
    public MerchantCouponVO create(CouponCreateRequest request) {
        if (request.discountAmount().compareTo(request.thresholdAmount()) >= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "减免金额必须小于使用门槛");
        }
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (!request.endAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于当前时间");
        }

        Coupon coupon = new Coupon();
        coupon.setShop(null);
        coupon.setTitle(request.title().trim());
        coupon.setThresholdAmount(request.thresholdAmount());
        coupon.setDiscountAmount(request.discountAmount());
        coupon.setTotalCount(request.totalCount());
        coupon.setClaimedCount(0);
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setEnabled(true);
        couponRepository.save(coupon);

        return MerchantCouponVO.from(coupon, 0L, LocalDateTime.now());
    }

    @Transactional
    public MerchantCouponVO updateStatus(Long couponId, boolean enabled) {
        Coupon coupon = couponRepository.findById(couponId)
                .filter(item -> item.getShop() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "平台券不存在"));
        coupon.setEnabled(enabled);

        long usedCount = userCouponRepository
                .countUsedByCouponIds(List.of(couponId), UserCouponStatus.USED).stream()
                .mapToLong(UserCouponRepository.CouponUsedCount::getTotal)
                .findFirst()
                .orElse(0L);
        return MerchantCouponVO.from(coupon, usedCount, LocalDateTime.now());
    }
}
