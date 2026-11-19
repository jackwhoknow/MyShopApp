package com.example.myshopapp.order;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.order.dto.BatchShipRequest;
import com.example.myshopapp.order.dto.BatchShipResultVO;
import com.example.myshopapp.order.dto.MerchantRemarkRequest;
import com.example.myshopapp.order.dto.MerchantSubOrderVO;
import com.example.myshopapp.order.dto.ShipRequest;
import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.dto.UpdateLogisticsRequest;
import com.example.myshopapp.order.entity.SubOrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 商家端订单履约接口，需要 MERCHANT 或 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    @GetMapping
    public ApiResponse<PageResult<MerchantSubOrderVO>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) SubOrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(merchantOrderService.list(loginUser.id(), status, keyword, startDate, endDate, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MerchantSubOrderVO> detail(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable Long id) {
        return ApiResponse.ok(merchantOrderService.detail(loginUser.id(), id));
    }

    @GetMapping("/{id}/logistics")
    public ApiResponse<SubOrderLogisticsVO> logistics(@AuthenticationPrincipal LoginUser loginUser,
                                                      @PathVariable Long id) {
        return ApiResponse.ok(merchantOrderService.logistics(loginUser.id(), id));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<MerchantSubOrderVO> ship(@AuthenticationPrincipal LoginUser loginUser,
                                                @PathVariable Long id,
                                                @Valid @RequestBody ShipRequest request) {
        return ApiResponse.ok(merchantOrderService.ship(loginUser.id(), id, request));
    }

    @PutMapping("/{id}/logistics")
    public ApiResponse<MerchantSubOrderVO> updateLogistics(@AuthenticationPrincipal LoginUser loginUser,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody UpdateLogisticsRequest request) {
        return ApiResponse.ok(merchantOrderService.updateLogistics(loginUser.id(), id, request));
    }

    @PutMapping("/{id}/remark")
    public ApiResponse<MerchantSubOrderVO> updateRemark(@AuthenticationPrincipal LoginUser loginUser,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody MerchantRemarkRequest request) {
        return ApiResponse.ok(merchantOrderService.updateRemark(loginUser.id(), id, request));
    }

    @PostMapping("/batch-ship")
    public ApiResponse<BatchShipResultVO> batchShip(@AuthenticationPrincipal LoginUser loginUser,
                                                    @Valid @RequestBody BatchShipRequest request) {
        return ApiResponse.ok(merchantOrderService.batchShip(loginUser.id(), request));
    }
}
