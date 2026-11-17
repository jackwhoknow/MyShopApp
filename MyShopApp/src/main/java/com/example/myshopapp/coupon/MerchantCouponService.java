package com.example.myshopapp.coupon;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.coupon.dto.CouponCreateRequest;
import com.example.myshopapp.coupon.dto.MerchantCouponVO;
import com.example.myshopapp.coupon.entity.Coupon;
import com.example.myshopapp.coupon.entity.UserCouponStatus;
import com.example.myshopapp.coupon.repository.CouponRepository;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家券管理：创建满减券、查看领取/使用情况、启用停用。
 */
@Service
@RequiredArgsConstructor
public class MerchantCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public List<MerchantCouponVO> list(Long ownerId) {
        Shop shop = requireShop(ownerId);
        Map<Long, Long> usedCounts = userCouponRepository
                .countUsedByShop(shop.getId(), UserCouponStatus.USED).stream()
                .collect(Collectors.toMap(UserCouponRepository.CouponUsedCount::getCouponId,
                        UserCouponRepository.CouponUsedCount::getTotal));

        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findByShopIdOrderByIdDesc(shop.getId()).stream()
                .map(coupon -> MerchantCouponVO.from(coupon, usedCounts.getOrDefault(coupon.getId(), 0L), now))
                .toList();
    }

    @Transactional
    public MerchantCouponVO create(Long ownerId, CouponCreateRequest request) {
        Shop shop = requireShop(ownerId);
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
        coupon.setShop(shop);
        coupon.setTitle(request.title().trim());
        coupon.setThresholdAmount(request.thresholdAmount());
        coupon.setDiscountAmount(request.discountAmount());
        coupon.setTotalCount(request.totalCount());
        coupon.setPointsPrice(request.pointsPrice() == null ? 0 : request.pointsPrice());
        coupon.setClaimedCount(0);
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setEnabled(true);
        couponRepository.save(coupon);

        return MerchantCouponVO.from(coupon, 0L, LocalDateTime.now());
    }

    @Transactional
    public MerchantCouponVO updateStatus(Long ownerId, Long couponId, boolean enabled) {
        Shop shop = requireShop(ownerId);
        Coupon coupon = couponRepository.findByIdAndShopId(couponId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在"));
        coupon.setEnabled(enabled);

        long usedCount = userCouponRepository
                .countUsedByShop(shop.getId(), UserCouponStatus.USED).stream()
                .filter(item -> item.getCouponId().equals(couponId))
                .mapToLong(UserCouponRepository.CouponUsedCount::getTotal)
                .findFirst()
                .orElse(0L);
        return MerchantCouponVO.from(coupon, usedCount, LocalDateTime.now());
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法管理优惠券"));
    }
}
