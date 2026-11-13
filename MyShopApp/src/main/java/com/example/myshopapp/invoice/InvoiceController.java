package com.example.myshopapp.invoice;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.invoice.dto.InvoiceRequest;
import com.example.myshopapp.invoice.dto.InvoiceVO;
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
 * 买家发票接口：按订单申请并查看开票状态。
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/orders/{orderId}")
    public ApiResponse<InvoiceVO> apply(@AuthenticationPrincipal LoginUser loginUser,
                                        @PathVariable Long orderId,
                                        @Valid @RequestBody InvoiceRequest request) {
        return ApiResponse.ok(invoiceService.apply(loginUser.id(), orderId, request));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<InvoiceVO> findByOrder(@AuthenticationPrincipal LoginUser loginUser,
                                              @PathVariable Long orderId) {
        return ApiResponse.ok(invoiceService.findByOrder(loginUser.id(), orderId));
    }

    @GetMapping
    public ApiResponse<PageResult<InvoiceVO>> mine(@AuthenticationPrincipal LoginUser loginUser,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(invoiceService.listMine(loginUser.id(), page, size));
    }
}
