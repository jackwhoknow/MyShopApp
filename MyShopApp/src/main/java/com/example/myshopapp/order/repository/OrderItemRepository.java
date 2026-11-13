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

    interface TopProductProjection {
        Long getProductId();

        String getProductName();

        Long getQuantity();

        BigDecimal getSalesAmount();
    }
}
