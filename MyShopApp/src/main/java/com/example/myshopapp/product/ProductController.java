package com.example.myshopapp.product;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.product.dto.ProductDetailVO;
import com.example.myshopapp.product.dto.ProductListItemVO;
import com.example.myshopapp.search.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 前台商品接口，游客可访问，只展示已上架商品。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SearchService searchService;

    @GetMapping
    public ApiResponse<PageResult<ProductListItemVO>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<ProductListItemVO> result = productService.publicList(
                keyword, categoryId, minPrice, maxPrice, sort, page, size);
        // 记录搜索词：热搜对所有人生效，历史只记录登录用户
        searchService.record(keyword, loginUser == null ? null : loginUser.id());
        return ApiResponse.ok(result);
    }

    /** 相关商品推荐：同分类热销 */
    @GetMapping("/{id}/related")
    public ApiResponse<List<ProductListItemVO>> related(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(productService.related(id, limit));
    }

    /** 首页推荐：全站热销 */
    @GetMapping("/recommended")
    public ApiResponse<List<ProductListItemVO>> recommended(
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(productService.recommended(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.publicDetail(id));
    }
}
