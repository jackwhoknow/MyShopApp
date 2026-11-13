package com.example.myshopapp.cart;

import com.example.myshopapp.cart.dto.CartAddRequest;
import com.example.myshopapp.cart.dto.CartItemVO;
import com.example.myshopapp.cart.dto.CartUpdateRequest;
import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口，全部基于当前登录用户。
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<List<CartItemVO>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(cartService.list(loginUser.id()));
    }

    @PostMapping
    public ApiResponse<CartItemVO> add(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody CartAddRequest request) {
        return ApiResponse.ok(cartService.add(loginUser.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CartItemVO> updateQuantity(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody CartUpdateRequest request) {
        return ApiResponse.ok(cartService.updateQuantity(loginUser.id(), id, request.quantity()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long id) {
        cartService.remove(loginUser.id(), id);
        return ApiResponse.ok();
    }

    @DeleteMapping
    public ApiResponse<Void> clear(@AuthenticationPrincipal LoginUser loginUser) {
        cartService.clear(loginUser.id());
        return ApiResponse.ok();
    }
}
