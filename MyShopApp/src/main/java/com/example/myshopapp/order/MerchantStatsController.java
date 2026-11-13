package com.example.myshopapp.order;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.order.dto.MerchantStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家经营看板接口，需要 MERCHANT 或 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/merchant/stats")
@RequiredArgsConstructor
public class MerchantStatsController {

    private final MerchantStatsService merchantStatsService;

    @GetMapping("/overview")
    public ApiResponse<MerchantStatsVO> overview(@AuthenticationPrincipal LoginUser loginUser,
                                                 @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(merchantStatsService.overview(loginUser.id(), days));
    }
}
