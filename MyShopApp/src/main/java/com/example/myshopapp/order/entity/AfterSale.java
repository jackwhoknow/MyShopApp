package com.example.myshopapp.order.entity;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后单：按子订单发起，一个子订单可以有多条历史记录。
 * 未发货走仅退款，已发货/已完成走退货退款。
 * userId / shopId 冗余存储，方便买家侧与商家侧分别分页查询。
 */
@Entity
@Table(name = "after_sale", indexes = {
        @Index(name = "idx_after_sale_sub_order", columnList = "sub_order_id"),
        @Index(name = "idx_after_sale_user", columnList = "user_id"),
        @Index(name = "idx_after_sale_shop", columnList = "shop_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AfterSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String afterSaleNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id", nullable = false)
    private SubOrder subOrder;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long shopId;

    /** 退款金额，取子订单金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 200)
    private String reason;

    /** 售后类型：历史数据可能为空，按仅退款处理 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AfterSaleType type = AfterSaleType.REFUND_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AfterSaleStatus status = AfterSaleStatus.PENDING;

    /** 商家处理备注：仅退款为处理说明，退货退款为退货地址/说明 */
    @Column(length = 200)
    private String merchantRemark;

    /** 平台介入备注，非空表示管理员已介入处理 */
    @Column(length = 200)
    private String adminRemark;

    /** 买家退货物流公司，退货退款寄回时填写 */
    @Column(length = 50)
    private String returnCompany;

    /** 买家退货运单号 */
    @Column(length = 50)
    private String returnTrackingNo;

    /** 买家寄回时间 */
    private LocalDateTime returnedAt;

    /** 商家确认收到退货的时间 */
    private LocalDateTime receivedAt;

    private LocalDateTime handledAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
