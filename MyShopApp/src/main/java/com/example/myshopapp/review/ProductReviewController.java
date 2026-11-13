package com.example.myshopapp.review;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.review.dto.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品评价公开接口：只返回审核通过的评价，游客可访问。
 */
@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<PageResult<ReviewVO>> list(@PathVariable Long productId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok(reviewService.listForProduct(productId, page, size));
    }
}
