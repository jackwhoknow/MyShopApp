package com.example.myshopapp.cart;

import com.example.myshopapp.cart.dto.CartAddRequest;
import com.example.myshopapp.cart.dto.CartItemVO;
import com.example.myshopapp.cart.entity.CartItem;
import com.example.myshopapp.cart.repository.CartItemRepository;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import com.example.myshopapp.seckill.SeckillActivityService;
import com.example.myshopapp.seckill.entity.SeckillActivity;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 购物车业务：同一 SKU 合并数量，加入前校验商品可售与库存。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserRepository userRepository;
    private final SeckillActivityService seckillActivityService;

    @Transactional
    public CartItemVO add(Long userId, CartAddRequest request) {
        ProductSku sku = requirePurchasableSku(request.skuId());

        CartItem item = cartItemRepository.findByUserIdAndSkuId(userId, sku.getId()).orElse(null);
        int current = item == null ? 0 : item.getQuantity();
        int merged = current + request.quantity();
        if (merged > 99) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "同一个商品最多购买 99 件");
        }
        if (merged > sku.getStock()) {
            throw new BusinessException(ErrorCode.CONFLICT, "库存不足，当前仅剩 " + sku.getStock() + " 件");
        }

        if (item == null) {
            item = new CartItem();
            item.setUser(userRepository.getReferenceById(userId));
            item.setSku(sku);
        }
        item.setQuantity(merged);
        cartItemRepository.save(item);
        return CartItemVO.from(item, activeActivity(sku.getId()));
    }

    @Transactional
    public CartItemVO updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = requireOwnedItem(userId, cartItemId);
        ProductSku sku = item.getSku();
        if (quantity > sku.getStock()) {
            throw new BusinessException(ErrorCode.CONFLICT, "库存不足，当前仅剩 " + sku.getStock() + " 件");
        }
        item.setQuantity(quantity);
        return CartItemVO.from(item, activeActivity(sku.getId()));
    }

    @Transactional
    public void remove(Long userId, Long cartItemId) {
        cartItemRepository.delete(requireOwnedItem(userId, cartItemId));
    }

    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteAll(cartItemRepository.findByUserIdOrderByIdDesc(userId));
    }

    @Transactional(readOnly = true)
    public List<CartItemVO> list(Long userId) {
        List<CartItem> items = cartItemRepository.findWithSkuByUserId(userId);
        List<Long> skuIds = items.stream().map(item -> item.getSku().getId()).toList();
        Map<Long, SeckillActivity> activities = skuIds.isEmpty()
                ? Map.of() : seckillActivityService.runningBySkuIds(skuIds);
        return items.stream()
                .map(item -> CartItemVO.from(item, activities.get(item.getSku().getId())))
                .toList();
    }

    /** 单个 SKU 当前生效的秒杀活动，没有则返回 null */
    private SeckillActivity activeActivity(Long skuId) {
        return seckillActivityService.runningBySkuIds(List.of(skuId)).get(skuId);
    }

    private CartItem requireOwnedItem(Long userId, Long cartItemId) {
        return cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "购物车项不存在"));
    }

    private ProductSku requirePurchasableSku(Long skuId) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品规格不存在"));
        Product product = sku.getProduct();
        if (product.getStatus() != ProductStatus.ON_SHELF) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品「" + product.getName() + "」已下架");
        }
        if (product.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "店铺「" + product.getShop().getName() + "」已暂停营业");
        }
        if (sku.getStock() <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已售罄");
        }
        return sku;
    }
}
