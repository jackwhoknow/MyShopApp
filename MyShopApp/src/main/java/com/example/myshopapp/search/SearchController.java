package com.example.myshopapp.search;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索增强接口：热搜、联想、个人搜索历史。
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/hot")
    public ApiResponse<List<String>> hot(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(searchService.hot(limit));
    }

    @GetMapping("/suggest")
    public ApiResponse<List<String>> suggest(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(searchService.suggest(keyword, limit));
    }

    @GetMapping("/history")
    public ApiResponse<List<String>> history(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(searchService.history(loginUser.id()));
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory(@AuthenticationPrincipal LoginUser loginUser) {
        searchService.clearHistory(loginUser.id());
        return ApiResponse.ok();
    }
}
