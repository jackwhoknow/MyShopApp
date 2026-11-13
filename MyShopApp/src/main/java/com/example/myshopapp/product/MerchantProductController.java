package com.example.myshopapp.product;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.product.dto.BatchProductStatusRequest;
import com.example.myshopapp.product.dto.BatchProductStatusResultVO;
import com.example.myshopapp.product.dto.ProductCreateRequest;
import com.example.myshopapp.product.dto.ProductDetailVO;
import com.example.myshopapp.product.dto.ProductListItemVO;
import com.example.myshopapp.product.dto.ProductStatusRequest;
import com.example.myshopapp.product.dto.ProductUpdateRequest;
import com.example.myshopapp.product.dto.SkuStockUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端商品接口。
 */
@RestController
@RequestMapping("/api/merchant/products")
@RequiredArgsConstructor
public class MerchantProductController {

    private final ProductService productService;

    @PostMapping
    public ApiResponse<ProductDetailVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                               @Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.ok(productService.create(loginUser.id(), request));
    }

    @GetMapping
    public ApiResponse<PageResult<ProductListItemVO>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(productService.merchantList(loginUser.id(), keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailVO> detail(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long id) {
        return ApiResponse.ok(productService.merchantDetail(loginUser.id(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailVO> update(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.ok(productService.update(loginUser.id(), id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ProductDetailVO> changeStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody ProductStatusRequest request) {
        return ApiResponse.ok(productService.changeStatus(loginUser.id(), id, request.status()));
    }

    @PutMapping("/{id}/skus")
    public ApiResponse<ProductDetailVO> updateSkus(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody SkuStockUpdateRequest request) {
        return ApiResponse.ok(productService.updateSkus(loginUser.id(), id, request));
    }

    @PutMapping("/batch-status")
    public ApiResponse<BatchProductStatusResultVO> batchChangeStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody BatchProductStatusRequest request) {
        return ApiResponse.ok(productService.batchChangeStatus(loginUser.id(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long id) {
        productService.delete(loginUser.id(), id);
        return ApiResponse.ok();
    }
}
