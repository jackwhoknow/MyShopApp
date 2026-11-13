package com.example.myshopapp.coupon;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.coupon.dto.CouponCreateRequest;
import com.example.myshopapp.coupon.dto.CouponStatusRequest;
import com.example.myshopapp.coupon.dto.MerchantCouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员平台券管理接口，需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @GetMapping
    public ApiResponse<List<MerchantCouponVO>> list() {
        return ApiResponse.ok(adminCouponService.listPlatform());
    }

    @PostMapping
    public ApiResponse<MerchantCouponVO> create(@Valid @RequestBody CouponCreateRequest request) {
        return ApiResponse.ok(adminCouponService.create(request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<MerchantCouponVO> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody CouponStatusRequest request) {
        return ApiResponse.ok(adminCouponService.updateStatus(id, request.enabled()));
    }
}
