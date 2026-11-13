package com.example.myshopapp.favorite;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.favorite.dto.FavoriteVO;
import com.example.myshopapp.favorite.entity.ProductFavorite;
import com.example.myshopapp.favorite.repository.ProductFavoriteRepository;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductRepository;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 商品收藏：收藏、取消收藏、我的收藏、列表页收藏状态。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final ProductFavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** 收藏商品：只允许收藏上架且店铺营业中的商品，重复收藏按成功处理 */
    @Transactional
    public void add(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (product.getStatus() != ProductStatus.ON_SHELF) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已下架，不能收藏");
        }
        if (product.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "店铺已暂停营业，暂不能收藏");
        }
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        ProductFavorite favorite = new ProductFavorite();
        favorite.setProduct(product);
        favorite.setUser(userRepository.getReferenceById(userId));
        favoriteRepository.save(favorite);
    }

    /** 取消收藏：没有收藏记录时按成功处理 */
    @Transactional
    public void remove(Long userId, Long productId) {
        favoriteRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public PageResult<FavoriteVO> mine(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductFavorite> favorites = favoriteRepository.findMine(userId, pageable);
        return PageResult.of(favorites.map(FavoriteVO::from));
    }

    /** 批量查询已收藏的商品 id，用于列表页标记心形状态 */
    @Transactional(readOnly = true)
    public List<Long> favoritedIds(Long userId, Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return favoriteRepository.findFavoritedProductIds(userId, productIds);
    }
}
