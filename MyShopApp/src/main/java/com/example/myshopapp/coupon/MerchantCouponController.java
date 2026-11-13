package com.example.myshopapp.coupon;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.coupon.dto.CouponCreateRequest;
import com.example.myshopapp.coupon.dto.CouponStatusRequest;
import com.example.myshopapp.coupon.dto.MerchantCouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家优惠券管理接口，需要 MERCHANT 或 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/merchant/coupons")
@RequiredArgsConstructor
public class MerchantCouponController {

    private final MerchantCouponService merchantCouponService;

    @GetMapping
    public ApiResponse<List<MerchantCouponVO>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(merchantCouponService.list(loginUser.id()));
    }

    @PostMapping
    public ApiResponse<MerchantCouponVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                                @Valid @RequestBody CouponCreateRequest request) {
        return ApiResponse.ok(merchantCouponService.create(loginUser.id(), request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<MerchantCouponVO> updateStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody CouponStatusRequest request) {
        return ApiResponse.ok(merchantCouponService.updateStatus(loginUser.id(), id, request.enabled()));
    }
}
