package com.example.myshopapp.order;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.order.dto.AfterSaleRequest;
import com.example.myshopapp.order.dto.AfterSaleVO;
import com.example.myshopapp.order.dto.ReturnShipmentRequest;
import jakarta.validation.Valid;
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
 * 买家售后接口：按子订单申请仅退款或退货退款、填写退货物流、查看自己的售后记录。
 */
@RestController
@RequestMapping("/api/after-sales")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @PostMapping
    public ApiResponse<AfterSaleVO> apply(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody AfterSaleRequest request) {
        return ApiResponse.ok(afterSaleService.apply(loginUser.id(), request));
    }

    /** 退货退款：买家填写寄回物流 */
    @PostMapping("/{id}/return-shipment")
    public ApiResponse<AfterSaleVO> returnShipment(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ReturnShipmentRequest request) {
        return ApiResponse.ok(afterSaleService.returnShipment(loginUser.id(), id, request));
    }

    @GetMapping
    public ApiResponse<PageResult<AfterSaleVO>> list(@AuthenticationPrincipal LoginUser loginUser,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(afterSaleService.listMine(loginUser.id(), page, size));
    }
}
