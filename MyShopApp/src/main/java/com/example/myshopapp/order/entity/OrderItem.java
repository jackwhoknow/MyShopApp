package com.example.myshopapp.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

/**
 * 订单商品快照：下单瞬间把商品名、规格、图片、单价全部固化，
 * 后续商品改价或下架都不影响历史订单。
 */
@Entity
@Table(name = "order_item", indexes = @Index(name = "idx_order_item_sub", columnList = "sub_order_id"))
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id", nullable = false)
    private SubOrder subOrder;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long skuId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(length = 100)
    private String skuDesc;

    @Column(length = 255)
    private String image;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
