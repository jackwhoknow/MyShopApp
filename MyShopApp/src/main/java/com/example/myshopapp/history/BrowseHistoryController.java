package com.example.myshopapp.history;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.history.dto.BrowseHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家浏览历史接口：记录浏览、最近浏览、清空历史。
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    @PostMapping("/{productId}")
    public ApiResponse<Void> record(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long productId) {
        browseHistoryService.record(loginUser.id(), productId);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<BrowseHistoryVO>> recent(@AuthenticationPrincipal LoginUser loginUser,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(browseHistoryService.recent(loginUser.id(), limit));
    }

    @DeleteMapping
    public ApiResponse<Void> clear(@AuthenticationPrincipal LoginUser loginUser) {
        browseHistoryService.clear(loginUser.id());
        return ApiResponse.ok();
    }
}
