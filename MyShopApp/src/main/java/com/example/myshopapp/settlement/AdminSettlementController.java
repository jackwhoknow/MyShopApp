package com.example.myshopapp.settlement;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.settlement.dto.AdminSettlementSummaryVO;
import com.example.myshopapp.settlement.dto.WithdrawalActionRequest;
import com.example.myshopapp.settlement.dto.WithdrawalVO;
import com.example.myshopapp.settlement.entity.WithdrawalStatus;
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
 * 平台结算审核接口，需要 ADMIN 角色：审核商家提现申请并打款。
 */
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @GetMapping("/summary")
    public ApiResponse<AdminSettlementSummaryVO> summary() {
        return ApiResponse.ok(adminSettlementService.summary());
    }

    @GetMapping("/withdrawals")
    public ApiResponse<PageResult<WithdrawalVO>> list(@RequestParam(required = false) WithdrawalStatus status,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminSettlementService.list(status, keyword, page, size));
    }

    @PostMapping("/withdrawals/{id}/approve")
    public ApiResponse<WithdrawalVO> approve(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) WithdrawalActionRequest request) {
        return ApiResponse.ok(adminSettlementService.approve(id, request == null ? null : request.remark()));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ApiResponse<WithdrawalVO> reject(@PathVariable Long id,
                                            @Valid @RequestBody(required = false) WithdrawalActionRequest request) {
        return ApiResponse.ok(adminSettlementService.reject(id, request == null ? null : request.remark()));
    }

    @PostMapping("/withdrawals/{id}/pay")
    public ApiResponse<WithdrawalVO> pay(@PathVariable Long id,
                                         @Valid @RequestBody(required = false) WithdrawalActionRequest request) {
        return ApiResponse.ok(adminSettlementService.pay(id, request == null ? null : request.remark()));
    }
}
