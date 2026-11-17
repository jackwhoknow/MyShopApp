package com.example.myshopapp.order.repository;

import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {

    /** 商家只能操作自己店铺的子订单 */
    Optional<SubOrder> findByIdAndShopId(Long id, Long shopId);

    /** 自动确认收货：已发货且发货时间早于截止时间的子订单 id */
    @Query("""
            select s.id from SubOrder s
            where s.status = :status
              and s.shippedAt is not null
              and s.shippedAt < :cutoff
            """)
    List<Long> findIdsByStatusAndShippedAtBefore(@Param("status") SubOrderStatus status,
                                                  @Param("cutoff") LocalDateTime cutoff);

    /** 买家只能对自己订单下的子订单发起售后 */
    Optional<SubOrder> findByIdAndOrderUserId(Long id, Long userId);

    /**
     * 商家履约列表：只展示已支付或已完成主订单下的子订单，
     * 支持按子订单状态、关键词（单号/收货人/手机号/商品名）和下单时间范围过滤。
     */
    @Query(value = """
            select s from SubOrder s
            where s.shop.id = :shopId
              and s.order.status in :orderStatuses
              and (:hasStatus = false or s.status = :status)
              and (:hasStart = false or s.createdAt >= :startAt)
              and (:hasEnd = false or s.createdAt < :endAt)
              and (:hasKeyword = false
                   or s.subOrderNo like :keywordPattern
                   or s.order.orderNo like :keywordPattern
                   or s.order.receiverName like :keywordPattern
                   or s.order.receiverPhone like :keywordPattern
                   or exists (select 1 from OrderItem i
                              where i.subOrder = s and i.productName like :keywordPattern))
            order by s.id desc
            """,
            countQuery = """
            select count(s) from SubOrder s
            where s.shop.id = :shopId
              and s.order.status in :orderStatuses
              and (:hasStatus = false or s.status = :status)
              and (:hasStart = false or s.createdAt >= :startAt)
              and (:hasEnd = false or s.createdAt < :endAt)
              and (:hasKeyword = false
                   or s.subOrderNo like :keywordPattern
                   or s.order.orderNo like :keywordPattern
                   or s.order.receiverName like :keywordPattern
                   or s.order.receiverPhone like :keywordPattern
                   or exists (select 1 from OrderItem i
                              where i.subOrder = s and i.productName like :keywordPattern))
            """)
    Page<SubOrder> searchByShop(@Param("shopId") Long shopId,
                                @Param("orderStatuses") Collection<OrderStatus> orderStatuses,
                                @Param("hasStatus") boolean hasStatus,
                                @Param("status") SubOrderStatus status,
                                @Param("hasKeyword") boolean hasKeyword,
                                @Param("keywordPattern") String keywordPattern,
                                @Param("hasStart") boolean hasStart,
                                @Param("startAt") LocalDateTime startAt,
                                @Param("hasEnd") boolean hasEnd,
                                @Param("endAt") LocalDateTime endAt,
                                Pageable pageable);

    /** 看板指标：指定子订单状态、且主订单处于给定状态的数量 */
    @Query("""
            select count(s) from SubOrder s
            where s.shop.id = :shopId
              and s.status = :status
              and s.order.status in :orderStatuses
            """)
    long countByShopAndStatus(@Param("shopId") Long shopId,
                              @Param("status") SubOrderStatus status,
                              @Param("orderStatuses") Collection<OrderStatus> orderStatuses);

    /** 今日新增子订单数：按创建时间统计，已取消的不计入 */
    @Query("""
            select count(s) from SubOrder s
            where s.shop.id = :shopId
              and s.createdAt >= :startAt
              and s.createdAt < :endAt
              and s.status <> :excludedStatus
            """)
    long countCreatedBetween(@Param("shopId") Long shopId,
                             @Param("startAt") LocalDateTime startAt,
                             @Param("endAt") LocalDateTime endAt,
                             @Param("excludedStatus") SubOrderStatus excludedStatus);

    /** 累计销售额：已发货 + 已完成的子订单金额之和，无数据时返回 null */
    @Query("""
            select sum(s.totalAmount) from SubOrder s
            where s.shop.id = :shopId
              and s.status in :statuses
            """)
    BigDecimal sumSalesAmount(@Param("shopId") Long shopId,
                              @Param("statuses") Collection<SubOrderStatus> statuses);

    /** 按主订单支付时间取轻量销售明细，供服务层在内存里补零并按天聚合 */
    @Query("""
            select s.totalAmount as totalAmount, s.order.paidAt as paidAt
            from SubOrder s
            where s.shop.id = :shopId
              and s.status in :statuses
              and s.order.paidAt >= :startAt
              and s.order.paidAt < :endAt
            """)
    List<SalesPointProjection> findSalesPoints(@Param("shopId") Long shopId,
                                               @Param("statuses") Collection<SubOrderStatus> statuses,
                                               @Param("startAt") LocalDateTime startAt,
                                               @Param("endAt") LocalDateTime endAt);

    /**
     * 可结算收入：已发货 + 已完成的子订单金额之和，
     * 且主订单已支付或已完成（已退款子订单为 CANCELLED，天然不计入）。
     */
    @Query("""
            select sum(s.totalAmount) from SubOrder s
            where s.shop.id = :shopId
              and s.status in :statuses
              and s.order.status in :orderStatuses
            """)
    BigDecimal sumSettleableAmount(@Param("shopId") Long shopId,
                                   @Param("statuses") Collection<SubOrderStatus> statuses,
                                   @Param("orderStatuses") Collection<OrderStatus> orderStatuses);

    /** 结算账单明细：店铺下已支付主订单（含已退款）的子订单，按 id 倒序 */
    @Query(value = """
            select s from SubOrder s
            where s.shop.id = :shopId
              and s.order.paidAt is not null
              and (:hasStatus = false or s.status = :status)
              and (:hasKeyword = false
                   or s.subOrderNo like :keywordPattern
                   or s.order.orderNo like :keywordPattern)
            order by s.id desc
            """,
            countQuery = """
            select count(s) from SubOrder s
            where s.shop.id = :shopId
              and s.order.paidAt is not null
              and (:hasStatus = false or s.status = :status)
              and (:hasKeyword = false
                   or s.subOrderNo like :keywordPattern
                   or s.order.orderNo like :keywordPattern)
            """)
    Page<SubOrder> searchSettlementBills(@Param("shopId") Long shopId,
                                         @Param("hasStatus") boolean hasStatus,
                                         @Param("status") SubOrderStatus status,
                                         @Param("hasKeyword") boolean hasKeyword,
                                         @Param("keywordPattern") String keywordPattern,
                                         Pageable pageable);

    /** 平台累计销售额：不区分店铺，已发货 + 已完成子订单金额之和 */
    @Query("""
            select sum(s.totalAmount) from SubOrder s
            where s.status in :statuses
            """)
    BigDecimal sumSalesAmountAll(@Param("statuses") Collection<SubOrderStatus> statuses);

    /** 平台今日新增子订单数：已取消的不计入 */
    @Query("""
            select count(s) from SubOrder s
            where s.createdAt >= :startAt
              and s.createdAt < :endAt
              and s.status <> :excludedStatus
            """)
    long countCreatedBetweenAll(@Param("startAt") LocalDateTime startAt,
                                @Param("endAt") LocalDateTime endAt,
                                @Param("excludedStatus") SubOrderStatus excludedStatus);

    /** 平台退款中数量：仅统计主订单仍有效的子订单 */
    @Query("""
            select count(s) from SubOrder s
            where s.status = :status
              and s.order.status in :orderStatuses
            """)
    long countByStatusAll(@Param("status") SubOrderStatus status,
                          @Param("orderStatuses") Collection<OrderStatus> orderStatuses);

    /** 平台近 N 天销售明细，供服务层补零并按天聚合 */
    @Query("""
            select s.totalAmount as totalAmount, s.order.paidAt as paidAt
            from SubOrder s
            where s.status in :statuses
              and s.order.paidAt >= :startAt
              and s.order.paidAt < :endAt
            """)
    List<SalesPointProjection> findSalesPointsAll(@Param("statuses") Collection<SubOrderStatus> statuses,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt);

    interface SalesPointProjection {
        BigDecimal getTotalAmount();

        LocalDateTime getPaidAt();
    }

    /** 订单报表导出：店铺指定时间范围内创建的子订单 */
    @Query("""
            select s from SubOrder s
            where s.shop.id = :shopId
              and s.createdAt >= :startAt
              and s.createdAt < :endAt
            order by s.id desc
            """)
    List<SubOrder> findForExport(@Param("shopId") Long shopId,
                                 @Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt);

    /** 结算账单导出：店铺指定时间范围内支付的子订单 */
    @Query("""
            select s from SubOrder s
            where s.shop.id = :shopId
              and s.order.paidAt >= :startAt
              and s.order.paidAt < :endAt
            order by s.order.paidAt asc, s.id asc
            """)
    List<SubOrder> findPaidForExport(@Param("shopId") Long shopId,
                                     @Param("startAt") LocalDateTime startAt,
                                     @Param("endAt") LocalDateTime endAt);
}
