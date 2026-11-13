package com.example.myshopapp.history.repository;

import com.example.myshopapp.history.entity.BrowseHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrowseHistoryRepository extends JpaRepository<BrowseHistory, Long> {

    Optional<BrowseHistory> findByUserIdAndProductId(Long userId, Long productId);

    /** 最近浏览：连同商品与店铺一起取，按浏览时间倒序 */
    @Query("""
            select h from BrowseHistory h
            join fetch h.product p
            join fetch p.shop
            where h.user.id = :userId
            order by h.viewedAt desc
            """)
    List<BrowseHistory> findRecent(@Param("userId") Long userId, Pageable pageable);

    void deleteByUserId(Long userId);

    /** 商品被删除时清理浏览记录，避免外键约束 */
    void deleteByProductId(Long productId);
}
