package com.example.myshopapp.promotion;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.promotion.dto.PromotionRequest;
import com.example.myshopapp.promotion.dto.PromotionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家满减活动管理。
 */
@RestController
@RequestMapping("/api/merchant/promotions")
@RequiredArgsConstructor
public class MerchantPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ApiResponse<List<PromotionVO>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(promotionService.merchantList(loginUser.id()));
    }

    @PostMapping
    public ApiResponse<PromotionVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.ok(promotionService.create(loginUser.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionVO> update(@AuthenticationPrincipal LoginUser loginUser,
                                           @PathVariable Long id,
                                           @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.ok(promotionService.update(loginUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long id) {
        promotionService.delete(loginUser.id(), id);
        return ApiResponse.ok();
    }
}
