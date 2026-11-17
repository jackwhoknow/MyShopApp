package com.example.myshopapp.promotion;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.promotion.dto.PromotionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台满减活动查询：店铺主页与商品页展示进行中的活动。
 */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ApiResponse<List<PromotionVO>> active(@RequestParam Long shopId) {
        return ApiResponse.ok(promotionService.activeForShop(shopId));
    }
}
