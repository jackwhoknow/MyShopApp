package com.example.myshopapp.product;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.product.dto.ProductDetailVO;
import com.example.myshopapp.product.dto.ProductListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 前台商品接口，游客可访问，只展示已上架商品。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResult<ProductListItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(productService.publicList(
                keyword, categoryId, minPrice, maxPrice, sort, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.publicDetail(id));
    }
}
