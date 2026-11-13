package com.example.myshopapp.favorite;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.favorite.dto.FavoriteVO;
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
 * 买家收藏接口：收藏、取消收藏、我的收藏、批量收藏状态。
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{productId}")
    public ApiResponse<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long productId) {
        favoriteService.add(loginUser.id(), productId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long productId) {
        favoriteService.remove(loginUser.id(), productId);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<PageResult<FavoriteVO>> mine(@AuthenticationPrincipal LoginUser loginUser,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(favoriteService.mine(loginUser.id(), page, size));
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> favoritedIds(@AuthenticationPrincipal LoginUser loginUser,
                                                @RequestParam List<Long> productIds) {
        return ApiResponse.ok(favoriteService.favoritedIds(loginUser.id(), productIds));
    }
}
