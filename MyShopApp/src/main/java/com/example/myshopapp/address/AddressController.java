package com.example.myshopapp.address;

import com.example.myshopapp.address.dto.AddressRequest;
import com.example.myshopapp.address.dto.AddressVO;
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
 * 收货地址接口。
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<AddressVO>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(addressService.list(loginUser.id()));
    }

    @PostMapping
    public ApiResponse<AddressVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.create(loginUser.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressVO> update(@AuthenticationPrincipal LoginUser loginUser,
                                         @PathVariable Long id,
                                         @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.update(loginUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long id) {
        addressService.delete(loginUser.id(), id);
        return ApiResponse.ok();
    }
}
