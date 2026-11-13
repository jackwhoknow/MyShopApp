package com.example.myshopapp.address.repository;

import com.example.myshopapp.address.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    /** 默认地址排在最前 */
    List<UserAddress> findByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("update UserAddress a set a.defaultAddress = false where a.user.id = :userId and a.defaultAddress = true")
    void clearDefault(@Param("userId") Long userId);
}
