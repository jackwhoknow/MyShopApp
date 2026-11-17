package com.example.myshopapp.order.repository;

import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);

    boolean existsBySkuId(Long skuId);

    /** 热销榜：按商品聚合销量与销售额，销量相同再按金额排序 */
    @Query("""
            select i.productId as productId,
                   max(i.productName) as productName,
                   sum(i.quantity) as quantity,
                   sum(i.totalPrice) as salesAmount
            from OrderItem i
            where i.subOrder.shop.id = :shopId
              and i.subOrder.status in :statuses
            group by i.productId
            order by sum(i.quantity) desc, sum(i.totalPrice) desc
            """)
    List<TopProductProjection> findTopProducts(@Param("shopId") Long shopId,
                                               @Param("statuses") Collection<SubOrderStatus> statuses,
                                               Pageable pageable);

    /** 平台热销榜：不区分店铺，按商品聚合销量与销售额 */
    @Query("""
            select i.productId as productId,
                   max(i.productName) as productName,
                   sum(i.quantity) as quantity,
                   sum(i.totalPrice) as salesAmount
            from OrderItem i
            where i.subOrder.status in :statuses
            group by i.productId
            order by sum(i.quantity) desc, sum(i.totalPrice) desc
            """)
    List<TopProductProjection> findTopProductsAll(@Param("statuses") Collection<SubOrderStatus> statuses,
                                                  Pageable pageable);

    /** 用户在某场秒杀活动中的已购数量（已取消的订单/子订单不计入限购） */
    @Query("""
            select coalesce(sum(i.quantity), 0) from OrderItem i
            where i.seckillActivityId = :activityId
              and i.subOrder.order.user.id = :userId
              and i.subOrder.order.status <> com.example.myshopapp.order.entity.OrderStatus.CANCELLED
              and i.subOrder.status <> com.example.myshopapp.order.entity.SubOrderStatus.CANCELLED
            """)
    long sumSeckillQuantity(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /** 用户已购买某商品的数量（限购校验；已取消的订单/子订单不计入） */
    @Query("""
            select coalesce(sum(i.quantity), 0) from OrderItem i
            where i.productId = :productId
              and i.subOrder.order.user.id = :userId
              and i.subOrder.order.status <> com.example.myshopapp.order.entity.OrderStatus.CANCELLED
              and i.subOrder.status <> com.example.myshopapp.order.entity.SubOrderStatus.CANCELLED
            """)
    long sumPurchasedQuantity(@Param("productId") Long productId, @Param("userId") Long userId);

    interface TopProductProjection {
        Long getProductId();

        String getProductName();

        Long getQuantity();

        BigDecimal getSalesAmount();
    }
}
