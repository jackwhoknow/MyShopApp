package com.example.myshopapp.review;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.review.dto.ReviewRequest;
import com.example.myshopapp.review.dto.ReviewVO;
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
 * 买家评价接口。
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                        @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.create(loginUser.id(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<ReviewVO>> mine(@AuthenticationPrincipal LoginUser loginUser,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.listMine(loginUser.id(), page, size));
    }
}
