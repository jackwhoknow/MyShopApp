package com.example.myshopapp.review.repository;

import com.example.myshopapp.review.entity.Review;
import com.example.myshopapp.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    Optional<Review> findByIdAndShopId(Long id, Long shopId);

    Page<Review> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Page<Review> findByShopIdOrderByIdDesc(Long shopId, Pageable pageable);

    /** 商品页只展示审核通过的评价 */
    Page<Review> findByProductIdAndStatusOrderByIdDesc(Long productId, ReviewStatus status, Pageable pageable);

    /** 平台评价审核列表：可按状态与关键词（评价内容/商品名）筛选 */
    @Query(value = """
            select r from Review r join r.orderItem i
            where (:status is null or r.status = :status)
              and (:keyword is null or r.content like %:keyword% or i.productName like %:keyword%)
            """,
            countQuery = """
            select count(r) from Review r join r.orderItem i
            where (:status is null or r.status = :status)
              and (:keyword is null or r.content like %:keyword% or i.productName like %:keyword%)
            """)
    Page<Review> searchForAdmin(@Param("status") ReviewStatus status,
                                @Param("keyword") String keyword,
                                Pageable pageable);
}
