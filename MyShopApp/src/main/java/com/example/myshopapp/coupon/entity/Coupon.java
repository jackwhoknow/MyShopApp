package com.example.myshopapp.coupon.entity;

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
 * 满减券模板：满 thresholdAmount 减 discountAmount，数量有限、限时领取。
 * shop 为空表示平台通用券，否则为店铺券。
 */
@Entity
@Table(name = "coupon", indexes = @Index(name = "idx_coupon_shop", columnList = "shop_id"))
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Column(nullable = false, length = 50)
    private String title;

    /** 使用门槛：该店铺商品原价合计需达到此金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal thresholdAmount;

    /** 减免金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** 发放总量 */
    @Column(nullable = false)
    private int totalCount;

    /** 已领取数量 */
    @Column(nullable = false)
    private int claimedCount;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    /** 停用只影响领取，已领取的券在有效期内仍可使用 */
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
