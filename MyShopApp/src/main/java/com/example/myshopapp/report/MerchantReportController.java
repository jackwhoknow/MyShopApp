package com.example.myshopapp.report;

import com.example.myshopapp.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 商家报表导出：订单明细与结算账单。
 */
@RestController
@RequestMapping("/api/merchant/reports")
@RequiredArgsConstructor
public class MerchantReportController {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MerchantReportService reportService;

    @GetMapping("/orders.csv")
    public ResponseEntity<byte[]> exportOrders(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return csv(reportService.exportOrders(loginUser.id(), startDate, endDate), "orders");
    }

    @GetMapping("/settlements.csv")
    public ResponseEntity<byte[]> exportSettlements(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return csv(reportService.exportSettlements(loginUser.id(), startDate, endDate), "settlements");
    }

    private ResponseEntity<byte[]> csv(byte[] content, String prefix) {
        String filename = prefix + "-" + LocalDate.now().format(FILE_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(content);
    }
}
