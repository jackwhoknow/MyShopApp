package com.example.myshopapp.notification;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.notification.dto.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知接口：我的通知、未读数、标记已读、全部已读。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResult<NotificationVO>> list(@AuthenticationPrincipal LoginUser loginUser,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(notificationService.list(loginUser.id(), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(notificationService.unreadCount(loginUser.id()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id) {
        notificationService.markRead(loginUser.id(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal LoginUser loginUser) {
        notificationService.markAllRead(loginUser.id());
        return ApiResponse.ok();
    }
}
