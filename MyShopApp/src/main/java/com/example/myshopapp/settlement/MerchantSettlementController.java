package com.example.myshopapp.settlement;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.settlement.dto.SettlementBillVO;
import com.example.myshopapp.settlement.dto.SettlementSummaryVO;
import com.example.myshopapp.settlement.dto.WithdrawalCreateRequest;
import com.example.myshopapp.settlement.dto.WithdrawalVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家结算接口：查看可提现余额、账单明细与提现记录，并发起提现。
 */
@RestController
@RequestMapping("/api/merchant/settlements")
@RequiredArgsConstructor
public class MerchantSettlementController {

    private final MerchantSettlementService merchantSettlementService;

    @GetMapping("/summary")
    public ApiResponse<SettlementSummaryVO> summary(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(merchantSettlementService.summary(loginUser.id()));
    }

    @GetMapping("/bills")
    public ApiResponse<PageResult<SettlementBillVO>> bills(@AuthenticationPrincipal LoginUser loginUser,
                                                           @RequestParam(required = false) SubOrderStatus status,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(merchantSettlementService.bills(loginUser.id(), status, keyword, page, size));
    }

    @GetMapping("/withdrawals")
    public ApiResponse<PageResult<WithdrawalVO>> withdrawals(@AuthenticationPrincipal LoginUser loginUser,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(merchantSettlementService.withdrawals(loginUser.id(), page, size));
    }

    @PostMapping("/withdrawals")
    public ApiResponse<WithdrawalVO> createWithdrawal(@AuthenticationPrincipal LoginUser loginUser,
                                                      @Valid @RequestBody WithdrawalCreateRequest request) {
        return ApiResponse.ok(merchantSettlementService.createWithdrawal(loginUser.id(), request));
    }
}
