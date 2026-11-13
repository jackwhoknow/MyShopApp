package com.example.myshopapp.product.repository;

import com.example.myshopapp.product.entity.ProductSku;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    /** 秒杀预扣前回源库存：一次性带出商品与店铺，避免懒加载 */
    @Query("select s from ProductSku s join fetch s.product p join fetch p.shop where s.id in :ids")
    List<ProductSku> findAllWithProductByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * 下单、取消订单时加悲观写锁，避免并发扣减/回补库存导致超卖。
     * 必须在事务中调用。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ProductSku s where s.id = :id")
    Optional<ProductSku> findByIdForUpdate(@Param("id") Long id);

    /** 商家补货：按 SKU 与所属店铺校验归属，并加行锁避免并发库存覆盖 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ProductSku s where s.id = :id and s.product.shop.id = :shopId")
    Optional<ProductSku> findByIdAndShopIdForUpdate(@Param("id") Long id, @Param("shopId") Long shopId);

    /** 库存预警列表：库存不高于阈值，按库存升序优先展示缺货商品 */
    @Query(value = """
            select s from ProductSku s
            where s.product.shop.id = :shopId
              and s.stock <= :threshold
              and (:hasKeyword = false or s.product.name like :keywordPattern)
            order by s.stock asc, s.id asc
            """,
            countQuery = """
            select count(s) from ProductSku s
            where s.product.shop.id = :shopId
              and s.stock <= :threshold
              and (:hasKeyword = false or s.product.name like :keywordPattern)
            """)
    Page<ProductSku> searchLowStock(@Param("shopId") Long shopId,
                                    @Param("threshold") int threshold,
                                    @Param("hasKeyword") boolean hasKeyword,
                                    @Param("keywordPattern") String keywordPattern,
                                    Pageable pageable);

    /** 缺货 SKU 数 */
    @Query("""
            select count(s) from ProductSku s
            where s.product.shop.id = :shopId and s.stock = 0
            """)
    long countOutOfStock(@Param("shopId") Long shopId);

    /** 低库存 SKU 数：有货但不超过阈值 */
    @Query("""
            select count(s) from ProductSku s
            where s.product.shop.id = :shopId
              and s.stock > 0 and s.stock <= :threshold
            """)
    long countLowStock(@Param("shopId") Long shopId, @Param("threshold") int threshold);

    /** 店铺 SKU 总数 */
    @Query("""
            select count(s) from ProductSku s
            where s.product.shop.id = :shopId
            """)
    long countByShop(@Param("shopId") Long shopId);
}
