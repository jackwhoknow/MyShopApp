package com.example.myshopapp.order.repository;

import com.example.myshopapp.order.entity.AfterSale;
import com.example.myshopapp.order.entity.AfterSaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AfterSaleRepository extends JpaRepository<AfterSale, Long> {

    Optional<AfterSale> findByIdAndShopId(Long id, Long shopId);

    /** 买家只能操作自己的售后单 */
    Optional<AfterSale> findByIdAndUserId(Long id, Long userId);

    Optional<AfterSale> findFirstBySubOrderIdOrderByIdDesc(Long subOrderId);

    /** 批量取子订单的最新售后单，结果按 id 倒序，调用方可取每个子订单的第一条 */
    List<AfterSale> findBySubOrderIdInOrderByIdDesc(Collection<Long> subOrderIds);

    boolean existsBySubOrderIdAndStatus(Long subOrderId, AfterSaleStatus status);

    /** 售后超时自动关闭：待处理且申请时间早于截止时间的售后单 id */
    @Query("""
            select a.id from AfterSale a
            where a.status = :status
              and a.createdAt < :cutoff
            """)
    List<Long> findIdsByStatusAndCreatedAtBefore(@Param("status") AfterSaleStatus status,
                                                  @Param("cutoff") LocalDateTime cutoff);

    /** 店铺已退款总额，用于结算概览展示 */
    @Query("""
            select sum(a.amount) from AfterSale a
            where a.shopId = :shopId
              and a.status = :status
            """)
    java.math.BigDecimal sumAmountByShopAndStatus(@Param("shopId") Long shopId,
                                                  @Param("status") AfterSaleStatus status);

    Page<AfterSale> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Page<AfterSale> findByShopIdOrderByIdDesc(Long shopId, Pageable pageable);

    Page<AfterSale> findByShopIdAndStatusOrderByIdDesc(Long shopId, AfterSaleStatus status, Pageable pageable);

    /**
     * 平台售后列表：管理员查看全部售后单，支持状态、关键词、申请时间范围过滤。
     * 关键词匹配售后单号/退款原因/子订单号/主订单号/店铺名/买家账号与昵称。
     */
    @Query(value = """
            select a from AfterSale a
            where (:hasStatus = false or a.status = :status)
              and (:hasStart = false or a.createdAt >= :startAt)
              and (:hasEnd = false or a.createdAt < :endAt)
              and (:hasKeyword = false
                   or a.afterSaleNo like :keywordPattern
                   or a.reason like :keywordPattern
                   or a.subOrder.subOrderNo like :keywordPattern
                   or a.subOrder.order.orderNo like :keywordPattern
                   or a.subOrder.shop.name like :keywordPattern
                   or a.subOrder.order.user.username like :keywordPattern
                   or a.subOrder.order.user.nickname like :keywordPattern)
            order by a.id desc
            """,
            countQuery = """
            select count(a) from AfterSale a
            where (:hasStatus = false or a.status = :status)
              and (:hasStart = false or a.createdAt >= :startAt)
              and (:hasEnd = false or a.createdAt < :endAt)
              and (:hasKeyword = false
                   or a.afterSaleNo like :keywordPattern
                   or a.reason like :keywordPattern
                   or a.subOrder.subOrderNo like :keywordPattern
                   or a.subOrder.order.orderNo like :keywordPattern
                   or a.subOrder.shop.name like :keywordPattern
                   or a.subOrder.order.user.username like :keywordPattern
                   or a.subOrder.order.user.nickname like :keywordPattern)
            """)
    Page<AfterSale> searchAdmin(@Param("hasStatus") boolean hasStatus,
                                @Param("status") AfterSaleStatus status,
                                @Param("hasKeyword") boolean hasKeyword,
                                @Param("keywordPattern") String keywordPattern,
                                @Param("hasStart") boolean hasStart,
                                @Param("startAt") LocalDateTime startAt,
                                @Param("hasEnd") boolean hasEnd,
                                @Param("endAt") LocalDateTime endAt,
                                Pageable pageable);
}
