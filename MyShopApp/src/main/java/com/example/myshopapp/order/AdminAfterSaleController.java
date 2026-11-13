package com.example.myshopapp.order;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.order.dto.AdminAfterSaleActionRequest;
import com.example.myshopapp.order.dto.AfterSaleVO;
import com.example.myshopapp.order.entity.AfterSaleStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 平台售后监管接口，需要 ADMIN 角色：跨店铺查看售后单并介入处理。
 */
@RestController
@RequestMapping("/api/admin/after-sales")
@RequiredArgsConstructor
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    @GetMapping
    public ApiResponse<PageResult<AfterSaleVO>> list(
            @RequestParam(required = false) AfterSaleStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(afterSaleService.listAll(status, keyword, startDate, endDate, page, size));
    }

    /** 平台介入退款：直接完成退款并回补库存 */
    @PostMapping("/{id}/refund")
    public ApiResponse<AfterSaleVO> refund(@PathVariable Long id,
                                           @Valid @RequestBody AdminAfterSaleActionRequest request) {
        return ApiResponse.ok(afterSaleService.platformRefund(id, request.remark()));
    }

    /** 平台介入关闭：终止售后且不退款 */
    @PostMapping("/{id}/close")
    public ApiResponse<AfterSaleVO> close(@PathVariable Long id,
                                          @Valid @RequestBody AdminAfterSaleActionRequest request) {
        return ApiResponse.ok(afterSaleService.platformClose(id, request.remark()));
    }
}
