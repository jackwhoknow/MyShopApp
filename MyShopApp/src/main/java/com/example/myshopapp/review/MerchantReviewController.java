package com.example.myshopapp.review;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.review.dto.ReviewReplyRequest;
import com.example.myshopapp.review.dto.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家评价管理接口。
 */
@RestController
@RequestMapping("/api/merchant/reviews")
@RequiredArgsConstructor
public class MerchantReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<PageResult<ReviewVO>> list(@AuthenticationPrincipal LoginUser loginUser,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok(reviewService.listForShop(loginUser.id(), page, size));
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<ReviewVO> reply(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.ok(reviewService.reply(loginUser.id(), id, request.reply()));
    }
}
