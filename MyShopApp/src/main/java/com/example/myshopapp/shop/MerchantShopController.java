package com.example.myshopapp.shop;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.shop.dto.ShopApplyRequest;
import com.example.myshopapp.shop.dto.ShopVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端店铺接口，需要 MERCHANT 角色。
 */
@RestController
@RequestMapping("/api/merchant/shops")
@RequiredArgsConstructor
public class MerchantShopController {

    private final ShopService shopService;

    @PostMapping("/apply")
    public ApiResponse<ShopVO> apply(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody ShopApplyRequest request) {
        return ApiResponse.ok(shopService.apply(loginUser.id(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<ShopVO> mine(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(shopService.getMine(loginUser.id()));
    }
}
