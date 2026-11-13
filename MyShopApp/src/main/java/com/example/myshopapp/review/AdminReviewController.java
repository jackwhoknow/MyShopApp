package com.example.myshopapp.review;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.review.dto.ReviewAuditRequest;
import com.example.myshopapp.review.dto.ReviewVO;
import com.example.myshopapp.review.entity.ReviewStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台评价审核：查看待审/全部评价，通过或驳回。
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<PageResult<ReviewVO>> list(@RequestParam(required = false) ReviewStatus status,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.listForAdmin(status, keyword, page, size));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ReviewVO> approve(@PathVariable Long id) {
        return ApiResponse.ok(reviewService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ReviewVO> reject(@PathVariable Long id,
                                        @Valid @RequestBody(required = false) ReviewAuditRequest request) {
        return ApiResponse.ok(reviewService.reject(id, request == null ? null : request.remark()));
    }
}
