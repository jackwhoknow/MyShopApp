package com.example.myshopapp.seckill.entity;

import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.shop.entity.Shop;
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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动：针对单个 SKU 设置活动时间、活动价、活动库存和每人限购。
 * 活动库存独立于 SKU 库存计数，下单时同时扣减两者。
 */
@Entity
@Table(name = "seckill_activity", indexes = {
        @Index(name = "idx_seckill_shop", columnList = "shop_id"),
        @Index(name = "idx_seckill_product", columnList = "product_id"),
        @Index(name = "idx_seckill_sku", columnList = "sku_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SeckillActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    /** 活动价，必须低于 SKU 原价 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal activityPrice;

    /** 活动库存总量 */
    @Column(nullable = false)
    private int totalStock;

    /** 已售数量 */
    @Column(nullable = false)
    private int soldCount;

    /** 每个用户在本活动内最多购买的数量 */
    @Column(nullable = false)
    private int perUserLimit;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    /** 停用后不再按活动价售卖，已生成的订单不受影响 */
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
