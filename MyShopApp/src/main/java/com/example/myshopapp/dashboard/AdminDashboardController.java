package com.example.myshopapp.dashboard;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.dashboard.dto.AdminDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台经营看板接口，需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public ApiResponse<AdminDashboardVO> overview(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(adminDashboardService.overview(days));
    }
}
