package com.example.myshopapp.invoice;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.invoice.dto.InvoiceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台发票管理：查看开票申请并标记已开具。
 */
@RestController
@RequestMapping("/api/admin/invoices")
@RequiredArgsConstructor
public class AdminInvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ApiResponse<PageResult<InvoiceVO>> list(@RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(invoiceService.listForAdmin(status, page, size));
    }

    @PostMapping("/{id}/issue")
    public ApiResponse<InvoiceVO> issue(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.issue(id));
    }
}
