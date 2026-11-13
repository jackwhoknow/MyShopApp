package com.example.myshopapp.shop.entity;

import com.example.myshopapp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 店铺：一个商家账号申请一家店，平台审核通过后才能经营。
 */
@Entity
@Table(name = "shop")
@Getter
@Setter
@NoArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 店主账号 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShopStatus status = ShopStatus.PENDING;

    /** 驳回原因，仅在 REJECTED 时有值 */
    @Column(length = 200)
    private String rejectReason;

    /** 审核时间，审核通过或驳回时写入 */
    private LocalDateTime auditedAt;

    /** 库存预警阈值：SKU 库存不高于该值时进入预警，商家可自行调整 */
    @Column(nullable = false, columnDefinition = "int default 5")
    private int lowStockThreshold = 5;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
