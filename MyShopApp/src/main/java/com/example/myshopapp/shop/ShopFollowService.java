package com.example.myshopapp.shop;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.shop.dto.FollowedShopVO;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.entity.ShopFollow;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.shop.repository.ShopFollowRepository;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 店铺关注：关注/取关、关注状态与关注列表。
 */
@Service
@RequiredArgsConstructor
public class ShopFollowService {

    private final ShopFollowRepository shopFollowRepository;
    private final ShopRepository shopRepository;

    @Transactional
    public boolean follow(Long userId, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在"));
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "店铺当前不可关注");
        }
        if (!shopFollowRepository.existsByUserIdAndShopId(userId, shopId)) {
            ShopFollow follow = new ShopFollow();
            follow.setUserId(userId);
            follow.setShopId(shopId);
            shopFollowRepository.save(follow);
        }
        return true;
    }

    @Transactional
    public boolean unfollow(Long userId, Long shopId) {
        shopFollowRepository.findByUserIdAndShopId(userId, shopId)
                .ifPresent(shopFollowRepository::delete);
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long userId, Long shopId) {
        return userId != null && shopFollowRepository.existsByUserIdAndShopId(userId, shopId);
    }

    @Transactional(readOnly = true)
    public long followers(Long shopId) {
        return shopFollowRepository.countByShopId(shopId);
    }

    @Transactional(readOnly = true)
    public List<FollowedShopVO> listMine(Long userId) {
        List<ShopFollow> follows = shopFollowRepository.findByUserIdOrderByIdDesc(userId);
        if (follows.isEmpty()) {
            return List.of();
        }
        Map<Long, Shop> shops = shopRepository.findAllById(
                        follows.stream().map(ShopFollow::getShopId).toList())
                .stream().collect(Collectors.toMap(Shop::getId, Function.identity()));
        return follows.stream()
                .map(follow -> {
                    Shop shop = shops.get(follow.getShopId());
                    if (shop == null) {
                        return null;
                    }
                    return new FollowedShopVO(shop.getId(), shop.getName(), shop.getDescription(),
                            shop.getStatus().name(), follow.getCreatedAt());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
