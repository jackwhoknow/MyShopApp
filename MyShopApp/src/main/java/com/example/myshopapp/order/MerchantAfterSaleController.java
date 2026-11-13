package com.example.myshopapp.order;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.order.dto.AfterSaleHandleRequest;
import com.example.myshopapp.order.dto.AfterSaleVO;
import com.example.myshopapp.order.entity.AfterSaleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家售后处理接口：查看本店售后单，同意退款或驳回。
 */
@RestController
@RequestMapping("/api/merchant/after-sales")
@RequiredArgsConstructor
public class MerchantAfterSaleController {

    private final AfterSaleService afterSaleService;

    @GetMapping
    public ApiResponse<PageResult<AfterSaleVO>> list(@AuthenticationPrincipal LoginUser loginUser,
                                                     @RequestParam(required = false) AfterSaleStatus status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(afterSaleService.listForShop(loginUser.id(), status, page, size));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<AfterSaleVO> approve(@AuthenticationPrincipal LoginUser loginUser,
                                            @PathVariable Long id,
                                            @RequestBody(required = false) AfterSaleHandleRequest request) {
        return ApiResponse.ok(afterSaleService.approve(loginUser.id(), id,
                request == null ? null : request.merchantRemark()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<AfterSaleVO> reject(@AuthenticationPrincipal LoginUser loginUser,
                                           @PathVariable Long id,
                                           @RequestBody(required = false) AfterSaleHandleRequest request) {
        return ApiResponse.ok(afterSaleService.reject(loginUser.id(), id,
                request == null ? null : request.merchantRemark()));
    }

    /** 退货退款：商家确认收到退货并退款 */
    @PostMapping("/{id}/confirm-receipt")
    public ApiResponse<AfterSaleVO> confirmReceipt(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(afterSaleService.confirmReceipt(loginUser.id(), id));
    }
}
