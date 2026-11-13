package com.example.myshopapp.address;

import com.example.myshopapp.address.dto.AddressRequest;
import com.example.myshopapp.address.dto.AddressVO;
import com.example.myshopapp.address.entity.UserAddress;
import com.example.myshopapp.address.repository.UserAddressRepository;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址簿业务：每个用户最多一个默认地址，第一条地址自动设为默认。
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressVO> list(Long userId) {
        return userAddressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
                .map(AddressVO::from)
                .toList();
    }

    @Transactional
    public AddressVO create(Long userId, AddressRequest request) {
        boolean first = userAddressRepository.countByUserId(userId) == 0;
        boolean asDefault = first || request.defaultAddress();
        if (asDefault) {
            userAddressRepository.clearDefault(userId);
        }

        UserAddress address = new UserAddress();
        address.setUser(userRepository.getReferenceById(userId));
        apply(address, request);
        address.setDefaultAddress(asDefault);
        userAddressRepository.save(address);
        return AddressVO.from(address);
    }

    @Transactional
    public AddressVO update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = requireOwned(userId, addressId);
        if (request.defaultAddress()) {
            userAddressRepository.clearDefault(userId);
        }
        apply(address, request);
        address.setDefaultAddress(request.defaultAddress());
        return AddressVO.from(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        UserAddress address = requireOwned(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        userAddressRepository.delete(address);
        userAddressRepository.flush();
        if (wasDefault) {
            List<UserAddress> rest = userAddressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId);
            if (!rest.isEmpty()) {
                rest.get(0).setDefaultAddress(true);
            }
        }
    }

    private void apply(UserAddress address, AddressRequest request) {
        address.setReceiverName(request.receiverName());
        address.setPhone(request.phone());
        address.setProvince(request.province());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setDetail(request.detail());
    }

    private UserAddress requireOwned(Long userId, Long addressId) {
        return userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在"));
    }
}
