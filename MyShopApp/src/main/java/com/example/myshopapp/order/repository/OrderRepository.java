package com.example.myshopapp.order.repository;

import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatusOrderByIdDesc(Long userId, OrderStatus status, Pageable pageable);

    /** 只能查看/操作自己的订单 */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /** 支付回调按商户订单号定位订单 */
    Optional<Order> findByOrderNo(String orderNo);

    /** 异步下单幂等：同一个排队请求最多生成一张订单 */
    Optional<Order> findByRequestId(String requestId);

    /** 平台有效订单数：已支付 + 已完成 */
    long countByStatusIn(Collection<OrderStatus> statuses);

    /** 超时未支付订单 id：创建时间早于截止时间，供定时任务批量取消 */
    @Query("""
            select o.id from Order o
            where o.status = :status
              and o.createdAt < :createdAt
            order by o.id
            """)
    List<Long> findIdsByStatusAndCreatedAtBefore(@Param("status") OrderStatus status,
                                                 @Param("createdAt") LocalDateTime createdAt);

    /**
     * 平台订单列表：管理员可查看全部订单，支持状态、关键词、下单时间范围过滤。
     * 关键词匹配主订单号/子订单号/收货人/手机号/买家账号/店铺名/商品名。
     */
    @Query(value = """
            select o from Order o
            where (:hasStatus = false or o.status = :status)
              and (:hasStart = false or o.createdAt >= :startAt)
              and (:hasEnd = false or o.createdAt < :endAt)
              and (:hasKeyword = false
                   or o.orderNo like :keywordPattern
                   or o.receiverName like :keywordPattern
                   or o.receiverPhone like :keywordPattern
                   or o.user.username like :keywordPattern
                   or o.user.nickname like :keywordPattern
                   or exists (select 1 from SubOrder s
                              where s.order = o and s.subOrderNo like :keywordPattern)
                   or exists (select 1 from SubOrder s
                              where s.order = o and s.shop.name like :keywordPattern)
                   or exists (select 1 from SubOrder s join s.items i
                              where s.order = o and i.productName like :keywordPattern))
            order by o.id desc
            """,
            countQuery = """
            select count(o) from Order o
            where (:hasStatus = false or o.status = :status)
              and (:hasStart = false or o.createdAt >= :startAt)
              and (:hasEnd = false or o.createdAt < :endAt)
              and (:hasKeyword = false
                   or o.orderNo like :keywordPattern
                   or o.receiverName like :keywordPattern
                   or o.receiverPhone like :keywordPattern
                   or o.user.username like :keywordPattern
                   or o.user.nickname like :keywordPattern
                   or exists (select 1 from SubOrder s
                              where s.order = o and s.subOrderNo like :keywordPattern)
                   or exists (select 1 from SubOrder s
                              where s.order = o and s.shop.name like :keywordPattern)
                   or exists (select 1 from SubOrder s join s.items i
                              where s.order = o and i.productName like :keywordPattern))
            """)
    Page<Order> searchAdmin(@Param("hasStatus") boolean hasStatus,
                            @Param("status") OrderStatus status,
                            @Param("hasKeyword") boolean hasKeyword,
                            @Param("keywordPattern") String keywordPattern,
                            @Param("hasStart") boolean hasStart,
                            @Param("startAt") LocalDateTime startAt,
                            @Param("hasEnd") boolean hasEnd,
                            @Param("endAt") LocalDateTime endAt,
                            Pageable pageable);
}
