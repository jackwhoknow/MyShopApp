package com.example.myshopapp.product;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.seckill.SeckillStockService;
import com.example.myshopapp.product.dto.LowStockThresholdRequest;
import com.example.myshopapp.product.dto.StockAlertSummaryVO;
import com.example.myshopapp.product.dto.StockAlertVO;
import com.example.myshopapp.product.dto.StockUpdateRequest;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 商家库存预警：按店铺阈值汇总低库存 SKU，并支持快速补货。
 */
@Service
@RequiredArgsConstructor
public class MerchantInventoryService {

    private final ShopRepository shopRepository;
    private final ProductSkuRepository productSkuRepository;
    private final SeckillStockService seckillStockService;

    @Transactional(readOnly = true)
    public PageResult<StockAlertVO> alerts(Long ownerId, String keyword, int page, int size) {
        Shop shop = requireShop(ownerId);
        Pageable pageable = PageRequest.of(page, size);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = normalizedKeyword != null;

        Page<ProductSku> skus = productSkuRepository.searchLowStock(
                shop.getId(),
                shop.getLowStockThreshold(),
                hasKeyword,
                hasKeyword ? "%" + normalizedKeyword + "%" : null,
                pageable);
        return PageResult.of(skus.map(StockAlertVO::from));
    }

    @Transactional(readOnly = true)
    public StockAlertSummaryVO summary(Long ownerId) {
        return buildSummary(requireShop(ownerId));
    }

    /** 调整预警阈值，返回调整后的概览 */
    @Transactional
    public StockAlertSummaryVO updateThreshold(Long ownerId, LowStockThresholdRequest request) {
        Shop shop = requireShop(ownerId);
        shop.setLowStockThreshold(request.threshold());
        return buildSummary(shop);
    }

    /** 快速补货：校验 SKU 属于当前店铺后直接更新库存 */
    @Transactional
    @CacheEvict(cacheNames = "product", key = "#result.productId()")
    public StockAlertVO restock(Long ownerId, Long skuId, StockUpdateRequest request) {
        Shop shop = requireShop(ownerId);
        ProductSku sku = productSkuRepository.findByIdAndShopIdForUpdate(skuId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在或不属于当前店铺"));
        // 补货按差额同步 Redis 预扣库存，正在排队的下单不会读到旧值
        seckillStockService.adjustAfterCommit(skuId, request.stock() - sku.getStock());
        sku.setStock(request.stock());
        return StockAlertVO.from(sku);
    }

    private StockAlertSummaryVO buildSummary(Shop shop) {
        int threshold = shop.getLowStockThreshold();
        return new StockAlertSummaryVO(
                productSkuRepository.countOutOfStock(shop.getId()),
                productSkuRepository.countLowStock(shop.getId(), threshold),
                productSkuRepository.countByShop(shop.getId()),
                threshold);
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法查看库存预警"));
    }
}
