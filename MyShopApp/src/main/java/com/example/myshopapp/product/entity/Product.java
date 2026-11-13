package com.example.myshopapp.product.entity;

import com.example.myshopapp.category.entity.Category;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品：属于一家店铺和一个分类，具体售卖组合放在 SKU 中。
 */
@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_shop", columnList = "shop_id"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String mainImage;

    /** 由 SKU 价格计算得出，列表页直接展示价格区间 */
    @Column(precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(nullable = false)
    private int sales = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    /** 管理员驳回理由，仅在审核驳回后展示给商家 */
    @Column(length = 200)
    private String rejectReason;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpec> specs = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSku> skus = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
