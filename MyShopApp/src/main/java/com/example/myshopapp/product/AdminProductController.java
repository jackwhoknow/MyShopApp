package com.example.myshopapp.product;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.product.dto.ProductDetailVO;
import com.example.myshopapp.product.dto.ProductListItemVO;
import com.example.myshopapp.product.dto.ProductRejectRequest;
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
 * 管理员商品审核接口。
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResult<ProductListItemVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(productService.adminList(status, keyword, page, size));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ProductDetailVO> approve(@PathVariable Long id) {
        return ApiResponse.ok(productService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ProductDetailVO> reject(@PathVariable Long id,
                                               @Valid @RequestBody ProductRejectRequest request) {
        return ApiResponse.ok(productService.reject(id, request.reason()));
    }
}
