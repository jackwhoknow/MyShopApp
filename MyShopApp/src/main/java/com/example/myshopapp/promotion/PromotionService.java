package com.example.myshopapp.promotion;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.promotion.dto.PromotionRequest;
import com.example.myshopapp.promotion.dto.PromotionVO;
import com.example.myshopapp.promotion.entity.ShopPromotion;
import com.example.myshopapp.promotion.repository.ShopPromotionRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 店铺满减：商家维护活动，下单时按店铺小计自动匹配优惠力度最大的活动。
 */
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final ShopPromotionRepository promotionRepository;
    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public List<PromotionVO> merchantList(Long ownerId) {
        Shop shop = requireShop(ownerId);
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findByShopIdOrderByIdDesc(shop.getId()).stream()
                .map(item -> PromotionVO.from(item, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PromotionVO> activeForShop(Long shopId) {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findActive(shopId, now).stream()
                .map(item -> PromotionVO.from(item, now))
                .toList();
    }

    @Transactional
    public PromotionVO create(Long ownerId, PromotionRequest request) {
        Shop shop = requireShop(ownerId);
        validate(request);
        ShopPromotion promotion = new ShopPromotion();
        promotion.setShop(shop);
        apply(promotion, request);
        return PromotionVO.from(promotionRepository.save(promotion), LocalDateTime.now());
    }

    @Transactional
    public PromotionVO update(Long ownerId, Long promotionId, PromotionRequest request) {
        Shop shop = requireShop(ownerId);
        ShopPromotion promotion = promotionRepository.findByIdAndShopId(promotionId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "满减活动不存在"));
        validate(request);
        apply(promotion, request);
        return PromotionVO.from(promotion, LocalDateTime.now());
    }

    @Transactional
    public void delete(Long ownerId, Long promotionId) {
        Shop shop = requireShop(ownerId);
        ShopPromotion promotion = promotionRepository.findByIdAndShopId(promotionId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "满减活动不存在"));
        promotionRepository.delete(promotion);
    }

    /**
     * 下单时匹配最优满减：门槛满足且优惠金额最大。
     */
    @Transactional(readOnly = true)
    public Optional<ShopPromotion> bestFor(Long shopId, BigDecimal subtotal) {
        return promotionRepository.findActive(shopId, LocalDateTime.now()).stream()
                .filter(item -> subtotal.compareTo(item.getThresholdAmount()) >= 0)
                .findFirst();
    }

    private void validate(PromotionRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (request.reduceAmount().compareTo(request.thresholdAmount()) >= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠金额必须小于满减门槛");
        }
    }

    private void apply(ShopPromotion promotion, PromotionRequest request) {
        promotion.setTitle(request.title().trim());
        promotion.setThresholdAmount(request.thresholdAmount());
        promotion.setReduceAmount(request.reduceAmount());
        promotion.setStartAt(request.startAt());
        promotion.setEndAt(request.endAt());
        promotion.setEnabled(request.enabled() == null || request.enabled());
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺"));
    }
}
