package com.example.myshopapp.order.entity;

import com.example.myshopapp.shop.entity.Shop;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 子订单：按店铺拆分，商家只看自己店铺的子订单。
 */
@Entity
@Table(name = "sub_order", indexes = {
        @Index(name = "idx_sub_order_order", columnList = "order_id"),
        @Index(name = "idx_sub_order_shop", columnList = "shop_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SubOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, length = 40)
    private String subOrderNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** 优惠券抵扣金额，0 表示未使用优惠券；totalAmount 为抵扣后的应付金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** 使用的优惠券名称快照，未使用优惠券时为空 */
    @Column(length = 50)
    private String couponTitle;

    /** 满减活动优惠金额，0 表示未命中活动 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal promotionDiscount = BigDecimal.ZERO;

    /** 命中的满减活动名称 */
    @Column(length = 50)
    private String promotionTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubOrderStatus status = SubOrderStatus.WAIT_SHIP;

    /** 物流公司，商家发货时填写 */
    @Column(length = 50)
    private String shipCompany;

    /** 运单号，商家发货时填写 */
    @Column(length = 50)
    private String trackingNo;

    private LocalDateTime shippedAt;

    private LocalDateTime completedAt;

    /** 商家内部备注，仅商家可见，不会展示给买家 */
    @Column(length = 200)
    private String merchantRemark;

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
