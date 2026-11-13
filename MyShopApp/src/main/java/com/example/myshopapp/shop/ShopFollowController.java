package com.example.myshopapp.shop;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.shop.dto.FollowedShopVO;
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
 * 店铺关注接口：关注/取关需要登录，关注状态查询允许游客访问（游客固定返回未关注）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShopFollowController {

    private final ShopFollowService shopFollowService;

    @PostMapping("/shops/{shopId}/follow")
    public ApiResponse<FollowState> follow(@AuthenticationPrincipal LoginUser loginUser,
                                           @PathVariable Long shopId) {
        shopFollowService.follow(loginUser.id(), shopId);
        return ApiResponse.ok(new FollowState(true, shopFollowService.followers(shopId)));
    }

    @DeleteMapping("/shops/{shopId}/follow")
    public ApiResponse<FollowState> unfollow(@AuthenticationPrincipal LoginUser loginUser,
                                             @PathVariable Long shopId) {
        shopFollowService.unfollow(loginUser.id(), shopId);
        return ApiResponse.ok(new FollowState(false, shopFollowService.followers(shopId)));
    }

    @GetMapping("/shops/follow-status")
    public ApiResponse<FollowState> status(@AuthenticationPrincipal LoginUser loginUser,
                                           @RequestParam Long shopId) {
        boolean following = shopFollowService.isFollowing(loginUser == null ? null : loginUser.id(), shopId);
        return ApiResponse.ok(new FollowState(following, shopFollowService.followers(shopId)));
    }

    @GetMapping("/me/followed-shops")
    public ApiResponse<List<FollowedShopVO>> mine(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(shopFollowService.listMine(loginUser.id()));
    }

    public record FollowState(boolean following, long followers) {
    }
}
