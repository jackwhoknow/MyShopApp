package com.example.myshopapp.order;

import com.example.myshopapp.cart.entity.CartItem;
import com.example.myshopapp.cart.repository.CartItemRepository;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.dto.ReorderResultVO;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单一键复购：把历史订单中的商品按当前库存重新加入购物车，
 * 已下架、售罄或库存不足的商品跳过并给出原因。
 */
@Service
@RequiredArgsConstructor
public class OrderReorderService {

    /** 与购物车保持一致的单品数量上限 */
    private static final int MAX_QUANTITY = 99;

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReorderResultVO reorder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));

        int addedCount = 0;
        List<ReorderResultVO.SkippedItem> skippedItems = new ArrayList<>();

        for (SubOrder subOrder : order.getSubOrders()) {
            for (OrderItem item : subOrder.getItems()) {
                String skipReason = addToCart(userId, item);
                if (skipReason == null) {
                    addedCount++;
                } else {
                    skippedItems.add(new ReorderResultVO.SkippedItem(
                            item.getProductName(), item.getSkuDesc(), skipReason));
                }
            }
        }

        return new ReorderResultVO(addedCount, skippedItems.size(), skippedItems);
    }

    /**
     * 尝试把单个订单商品加入购物车。
     *
     * @return 成功返回 null，失败返回跳过原因
     */
    private String addToCart(Long userId, OrderItem item) {
        ProductSku sku = productSkuRepository.findById(item.getSkuId()).orElse(null);
        if (sku == null) {
            return "商品已不存在";
        }
        Product product = sku.getProduct();
        if (product.getStatus() != ProductStatus.ON_SHELF) {
            return "商品已下架";
        }
        if (product.getShop().getStatus() != ShopStatus.ACTIVE) {
            return "店铺已暂停营业";
        }
        if (sku.getStock() <= 0) {
            return "商品已售罄";
        }
        if (sku.getStock() < item.getQuantity()) {
            return "库存不足，当前仅剩 " + sku.getStock() + " 件";
        }

        CartItem cartItem = cartItemRepository.findByUserIdAndSkuId(userId, sku.getId()).orElse(null);
        int current = cartItem == null ? 0 : cartItem.getQuantity();
        int merged = current + item.getQuantity();
        if (merged > MAX_QUANTITY) {
            return "购物车数量将超过 " + MAX_QUANTITY + " 件上限";
        }
        if (merged > sku.getStock()) {
            return "库存不足，当前仅剩 " + sku.getStock() + " 件";
        }

        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setUser(userRepository.getReferenceById(userId));
            cartItem.setSku(sku);
        }
        cartItem.setQuantity(merged);
        cartItemRepository.save(cartItem);
        return null;
    }
}
