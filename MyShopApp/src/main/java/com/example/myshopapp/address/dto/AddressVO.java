package com.example.myshopapp.address.dto;

import com.example.myshopapp.address.entity.UserAddress;

/**
 * 地址展示项。
 */
public record AddressVO(
        Long id,
        String receiverName,
        String phone,
        String province,
        String city,
        String district,
        String detail,
        String fullAddress,
        boolean defaultAddress
) {

    public static AddressVO from(UserAddress address) {
        return new AddressVO(
                address.getId(),
                address.getReceiverName(),
                address.getPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetail(),
                address.fullAddress(),
                address.isDefaultAddress());
    }
}
