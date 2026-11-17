package com.example.myshopapp.promotion;

import com.example.myshopapp.promotion.entity.ShopPromotion;
import com.example.myshopapp.promotion.repository.ShopPromotionRepository;
import com.example.myshopapp.shop.repository.ShopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 满减匹配规则：满足门槛时取优惠金额最大的活动，不满足门槛则不参与。
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    private static final Long SHOP_ID = 3L;

    @Mock
    private ShopPromotionRepository promotionRepository;
    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private PromotionService promotionService;

    @Test
    @DisplayName("多个活动命中门槛时选择优惠金额最大的一个")
    void bestForPicksLargestDiscount() {
        ShopPromotion small = promotion("满 100 减 10", 100, 10);
        ShopPromotion large = promotion("满 200 减 30", 200, 30);
        // 仓储按优惠金额倒序返回
        when(promotionRepository.findActive(eq(SHOP_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(large, small));

        Optional<ShopPromotion> result = promotionService.bestFor(SHOP_ID, new BigDecimal("250"));

        assertThat(result).contains(large);
    }

    @Test
    @DisplayName("只有更高门槛的活动时，按小计继续向下匹配")
    void bestForSkipsUnmetThreshold() {
        ShopPromotion small = promotion("满 100 减 10", 100, 10);
        ShopPromotion large = promotion("满 200 减 30", 200, 30);
        when(promotionRepository.findActive(eq(SHOP_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(large, small));

        Optional<ShopPromotion> result = promotionService.bestFor(SHOP_ID, new BigDecimal("150"));

        assertThat(result).contains(small);
    }

    @Test
    @DisplayName("未达到任何门槛时不返回活动")
    void bestForReturnsEmptyWhenBelowThreshold() {
        ShopPromotion small = promotion("满 100 减 10", 100, 10);
        when(promotionRepository.findActive(eq(SHOP_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(small));

        Optional<ShopPromotion> result = promotionService.bestFor(SHOP_ID, new BigDecimal("99.99"));

        assertThat(result).isEmpty();
    }

    private ShopPromotion promotion(String title, double threshold, double reduce) {
        ShopPromotion promotion = new ShopPromotion();
        promotion.setTitle(title);
        promotion.setThresholdAmount(BigDecimal.valueOf(threshold));
        promotion.setReduceAmount(BigDecimal.valueOf(reduce));
        promotion.setEnabled(true);
        promotion.setStartAt(LocalDateTime.now().minusDays(1));
        promotion.setEndAt(LocalDateTime.now().plusDays(1));
        return promotion;
    }
}
