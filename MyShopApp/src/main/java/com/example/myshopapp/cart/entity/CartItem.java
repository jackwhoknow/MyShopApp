package com.example.myshopapp.cart.entity;

import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.user.entity.User;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 购物车项：只记录用户、SKU 和数量，价格与库存始终以 SKU 实时数据为准。
 */
@Entity
@Table(name = "cart_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_user_sku", columnNames = {"user_id", "sku_id"}),
        indexes = @Index(name = "idx_cart_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    @Column(nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
