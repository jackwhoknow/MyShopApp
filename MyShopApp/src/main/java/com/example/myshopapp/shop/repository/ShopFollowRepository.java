package com.example.myshopapp.shop.repository;

import com.example.myshopapp.shop.entity.ShopFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopFollowRepository extends JpaRepository<ShopFollow, Long> {

    boolean existsByUserIdAndShopId(Long userId, Long shopId);

    Optional<ShopFollow> findByUserIdAndShopId(Long userId, Long shopId);

    long countByShopId(Long shopId);

    List<ShopFollow> findByUserIdOrderByIdDesc(Long userId);
}
