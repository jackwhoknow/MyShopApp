package com.example.myshopapp.favorite.repository;

import com.example.myshopapp.favorite.entity.ProductFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<ProductFavorite> findByUserIdAndProductId(Long userId, Long productId);

    /** 我的收藏：连同商品与店铺一起取，商品下架后仍保留记录 */
    @Query(value = """
            select f from ProductFavorite f
            join fetch f.product p
            join fetch p.shop
            where f.user.id = :userId
            order by f.id desc
            """,
            countQuery = """
                    select count(f) from ProductFavorite f
                    where f.user.id = :userId
                    """)
    Page<ProductFavorite> findMine(@Param("userId") Long userId, Pageable pageable);

    /** 列表页批量判断哪些商品已被当前用户收藏 */
    @Query("""
            select f.product.id from ProductFavorite f
            where f.user.id = :userId and f.product.id in :productIds
            """)
    List<Long> findFavoritedProductIds(@Param("userId") Long userId,
                                       @Param("productIds") Collection<Long> productIds);

    /** 商品被删除时清理收藏记录，避免外键约束 */
    void deleteByProductId(Long productId);
}
