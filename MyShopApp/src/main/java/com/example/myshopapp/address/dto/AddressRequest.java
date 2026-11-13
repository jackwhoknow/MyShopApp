package com.example.myshopapp.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增/修改收货地址共用。
 */
public record AddressRequest(
        @NotBlank(message = "收货人不能为空") @Size(max = 50) String receiverName,
        @NotBlank(message = "手机号不能为空") @Size(max = 20) String phone,
        @NotBlank(message = "省份不能为空") @Size(max = 50) String province,
        @NotBlank(message = "城市不能为空") @Size(max = 50) String city,
        @NotBlank(message = "区县不能为空") @Size(max = 50) String district,
        @NotBlank(message = "详细地址不能为空") @Size(max = 200) String detail,
        boolean defaultAddress
) {
}
