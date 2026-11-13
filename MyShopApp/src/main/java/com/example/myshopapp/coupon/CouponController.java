package com.example.myshopapp.coupon;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.coupon.dto.CouponVO;
import com.example.myshopapp.coupon.dto.UserCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家优惠券接口：领券中心、领券、我的券。
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/available")
    public ApiResponse<List<CouponVO>> available(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(couponService.available(loginUser.id()));
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<UserCouponVO> claim(@AuthenticationPrincipal LoginUser loginUser,
                                           @PathVariable Long id) {
        return ApiResponse.ok(couponService.claim(loginUser.id(), id));
    }

    @GetMapping("/mine")
    public ApiResponse<List<UserCouponVO>> mine(@AuthenticationPrincipal LoginUser loginUser,
                                                @RequestParam(required = false) String state) {
        return ApiResponse.ok(couponService.mine(loginUser.id(), state));
    }
}
