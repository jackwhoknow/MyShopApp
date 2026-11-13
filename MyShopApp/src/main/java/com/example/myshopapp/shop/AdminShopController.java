package com.example.myshopapp.shop;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.shop.dto.ShopRejectRequest;
import com.example.myshopapp.shop.dto.ShopVO;
import com.example.myshopapp.shop.entity.ShopStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端店铺审核接口，需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private final ShopService shopService;

    /** status 不传则查询全部 */
    @GetMapping
    public ApiResponse<PageResult<ShopVO>> list(
            @RequestParam(required = false) ShopStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(shopService.list(status, page, size));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ShopVO> approve(@PathVariable Long id) {
        return ApiResponse.ok(shopService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ShopVO> reject(@PathVariable Long id,
                                      @Valid @RequestBody ShopRejectRequest request) {
        return ApiResponse.ok(shopService.reject(id, request.reason()));
    }
}
