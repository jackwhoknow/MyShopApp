package com.example.myshopapp.product;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.product.dto.LowStockThresholdRequest;
import com.example.myshopapp.product.dto.StockAlertSummaryVO;
import com.example.myshopapp.product.dto.StockAlertVO;
import com.example.myshopapp.product.dto.StockUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家库存预警接口，需要 MERCHANT 或 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/merchant/inventory")
@RequiredArgsConstructor
public class MerchantInventoryController {

    private final MerchantInventoryService merchantInventoryService;

    @GetMapping("/alerts")
    public ApiResponse<PageResult<StockAlertVO>> alerts(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(merchantInventoryService.alerts(loginUser.id(), keyword, page, size));
    }

    @GetMapping("/summary")
    public ApiResponse<StockAlertSummaryVO> summary(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(merchantInventoryService.summary(loginUser.id()));
    }

    @PutMapping("/threshold")
    public ApiResponse<StockAlertSummaryVO> updateThreshold(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody LowStockThresholdRequest request) {
        return ApiResponse.ok(merchantInventoryService.updateThreshold(loginUser.id(), request));
    }

    @PutMapping("/skus/{skuId}/stock")
    public ApiResponse<StockAlertVO> restock(@AuthenticationPrincipal LoginUser loginUser,
                                             @PathVariable Long skuId,
                                             @Valid @RequestBody StockUpdateRequest request) {
        return ApiResponse.ok(merchantInventoryService.restock(loginUser.id(), skuId, request));
    }
}
