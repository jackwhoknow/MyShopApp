package com.example.myshopapp.product.repository;

import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    /** 平台看板：按商品状态统计数量 */
    long countByStatus(ProductStatus status);

    Optional<Product> findByIdAndShopOwnerId(Long id, Long ownerId);

    boolean existsByCategoryId(Long categoryId);

    /** 前台商品列表：关键词为空时查全部；可按价格区间筛选，列表项需要店铺名，这里 join fetch 避免 N+1 */
    @Query(value = """
            select p from Product p join fetch p.shop
            where p.status = :status
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:minPrice is null or p.maxPrice >= :minPrice)
              and (:maxPrice is null or p.minPrice <= :maxPrice)
            """,
            countQuery = """
            select count(p) from Product p
            where p.status = :status
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:minPrice is null or p.maxPrice >= :minPrice)
              and (:maxPrice is null or p.minPrice <= :maxPrice)
            """)
    Page<Product> searchPublic(@Param("status") ProductStatus status,
                               @Param("keyword") String keyword,
                               @Param("minPrice") BigDecimal minPrice,
                               @Param("maxPrice") BigDecimal maxPrice,
                               Pageable pageable);

    @Query(value = """
            select p from Product p join fetch p.shop
            where p.status = :status
              and p.category.id in :categoryIds
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:minPrice is null or p.maxPrice >= :minPrice)
              and (:maxPrice is null or p.minPrice <= :maxPrice)
            """,
            countQuery = """
            select count(p) from Product p
            where p.status = :status
              and p.category.id in :categoryIds
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:minPrice is null or p.maxPrice >= :minPrice)
              and (:maxPrice is null or p.minPrice <= :maxPrice)
            """)
    Page<Product> searchPublicInCategories(@Param("status") ProductStatus status,
                                           @Param("categoryIds") Collection<Long> categoryIds,
                                           @Param("keyword") String keyword,
                                           @Param("minPrice") BigDecimal minPrice,
                                           @Param("maxPrice") BigDecimal maxPrice,
                                           Pageable pageable);

    /** 商家后台商品列表，包含草稿和已下架商品 */
    @Query(value = """
            select p from Product p join fetch p.shop
            where p.shop.id = :shopId
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(p) from Product p
            where p.shop.id = :shopId
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Product> searchByShop(@Param("shopId") Long shopId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    /** 店铺主页商品列表：只返回该店指定状态的商品，支持关键词 */
    @Query(value = """
            select p from Product p join fetch p.shop join fetch p.category
            where p.shop.id = :shopId
              and p.status = :status
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(p) from Product p
            where p.shop.id = :shopId
              and p.status = :status
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Product> searchPublicByShop(@Param("shopId") Long shopId,
                                     @Param("status") ProductStatus status,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);

    /** 店铺在售商品数 */
    long countByShopIdAndStatus(Long shopId, ProductStatus status);

    /** 管理员商品审核列表：status 为空时查全部状态 */
    @Query(value = """
            select p from Product p join fetch p.shop join fetch p.category
            where (:status is null or p.status = :status)
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(p) from Product p
            where (:status is null or p.status = :status)
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Product> searchAdmin(@Param("status") ProductStatus status,
                              @Param("keyword") String keyword,
                              Pageable pageable);
}
