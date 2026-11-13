package com.example.myshopapp.shop;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.product.ProductService;
import com.example.myshopapp.product.dto.ProductListItemVO;
import com.example.myshopapp.shop.dto.ShopPublicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺公开接口：买家查看店铺主页与店内商品，游客也可访问。
 */
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopPublicController {

    private final ShopService shopService;
    private final ProductService productService;

    @GetMapping("/{shopId}")
    public ApiResponse<ShopPublicVO> detail(@PathVariable Long shopId) {
        return ApiResponse.ok(shopService.publicDetail(shopId));
    }

    /** 店内商品列表：先校验店铺营业状态，避免通过 URL 访问到已关闭店铺的商品 */
    @GetMapping("/{shopId}/products")
    public ApiResponse<PageResult<ProductListItemVO>> products(
            @PathVariable Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        shopService.publicDetail(shopId);
        return ApiResponse.ok(productService.publicListByShop(shopId, keyword, sort, page, size));
    }
}
