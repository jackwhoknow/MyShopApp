package com.example.myshopapp.order;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.order.dto.AdminOrderDetailVO;
import com.example.myshopapp.order.dto.AdminOrderVO;
import com.example.myshopapp.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 平台端订单监管接口，需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ApiResponse<PageResult<AdminOrderVO>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminOrderService.list(status, keyword, startDate, endDate, page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String csv = adminOrderService.exportCsv(status, keyword, startDate, endDate);
        String filename = "orders-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminOrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminOrderService.detail(id));
    }
}
