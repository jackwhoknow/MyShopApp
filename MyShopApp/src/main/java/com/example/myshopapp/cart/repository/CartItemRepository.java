package com.example.myshopapp.cart.repository;

import com.example.myshopapp.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    List<CartItem> findByUserIdOrderByIdDesc(Long userId);

    List<CartItem> findByUserIdAndIdIn(Long userId, Collection<Long> ids);

    /** 删除商品前清理购物车中引用这些 SKU 的条目 */
    void deleteBySkuIdIn(Collection<Long> skuIds);

    /** 购物车列表要展示商品和店铺信息，这里一次 join fetch 取齐，避免 N+1 */
    @Query("""
            select c from CartItem c
            join fetch c.sku s
            join fetch s.product p
            join fetch p.shop
            where c.user.id = :userId
            order by c.id desc
            """)
    List<CartItem> findWithSkuByUserId(@Param("userId") Long userId);
}
