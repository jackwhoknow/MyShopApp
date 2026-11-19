package com.example.myshopapp.coupon;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.coupon.dto.UserCouponVO;
import com.example.myshopapp.coupon.entity.Coupon;
import com.example.myshopapp.coupon.entity.UserCoupon;
import com.example.myshopapp.coupon.repository.CouponRepository;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 领券规则：有效券可领，已领完与重复领取必须被拒绝。 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    private static final long USER_ID = 7L;
    private static final long COUPON_ID = 11L;

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("领券成功：已领数量加一，生成未使用的用户券")
    void claimIncreasesCountAndPersistsUserCoupon() {
        Coupon coupon = claimableCoupon();
        when(couponRepository.findByIdForUpdate(COUPON_ID)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByCouponIdAndUserId(COUPON_ID, USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(userCouponRepository.save(any(UserCoupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserCouponVO result = couponService.claim(USER_ID, COUPON_ID);

        assertThat(coupon.getClaimedCount()).isEqualTo(1);
        assertThat(result.couponId()).isEqualTo(COUPON_ID);
        assertThat(result.state()).isEqualTo("UNUSED");
        verify(userCouponRepository).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("券已领完：拒绝领取且不写入用户券")
    void claimRejectsSoldOutCoupon() {
        Coupon coupon = claimableCoupon();
        coupon.setClaimedCount(coupon.getTotalCount());
        when(couponRepository.findByIdForUpdate(COUPON_ID)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.claim(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已领完");
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("重复领取：同一用户不能领取同一张券两次")
    void claimRejectsDuplicateCoupon() {
        when(couponRepository.findByIdForUpdate(COUPON_ID))
                .thenReturn(Optional.of(claimableCoupon()));
        when(userCouponRepository.existsByCouponIdAndUserId(COUPON_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> couponService.claim(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("领取过");
        verify(userCouponRepository, never()).save(any());
    }

    private Coupon claimableCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(COUPON_ID);
        coupon.setTitle("测试券");
        coupon.setThresholdAmount(new BigDecimal("100.00"));
        coupon.setDiscountAmount(new BigDecimal("10.00"));
        coupon.setTotalCount(10);
        coupon.setStartAt(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        coupon.setEnabled(true);
        return coupon;
    }
}
