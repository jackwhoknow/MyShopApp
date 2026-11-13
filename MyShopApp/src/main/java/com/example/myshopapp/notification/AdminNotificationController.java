package com.example.myshopapp.notification;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.notification.dto.AdminBroadcastRequest;
import com.example.myshopapp.notification.dto.AdminBroadcastVO;
import com.example.myshopapp.notification.dto.AudienceCountsVO;
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
 * 平台端通知群发接口，需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @PostMapping("/broadcast")
    public ApiResponse<AdminBroadcastVO> broadcast(@AuthenticationPrincipal LoginUser loginUser,
                                                   @Valid @RequestBody AdminBroadcastRequest request) {
        return ApiResponse.ok(adminNotificationService.broadcast(loginUser.id(), loginUser.username(), request));
    }

    @GetMapping("/broadcast")
    public ApiResponse<PageResult<AdminBroadcastVO>> history(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminNotificationService.history(page, size));
    }

    @GetMapping("/audience-counts")
    public ApiResponse<AudienceCountsVO> audienceCounts() {
        return ApiResponse.ok(adminNotificationService.audienceCounts());
    }
}
