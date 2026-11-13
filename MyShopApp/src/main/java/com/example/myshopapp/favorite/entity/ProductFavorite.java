package com.example.myshopapp.favorite.entity;

import com.example.myshopapp.product.entity.Product;
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

import java.time.LocalDateTime;

/**
 * 商品收藏：同一用户对同一商品只能收藏一次。
 */
@Entity
@Table(name = "product_favorite",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_favorite",
                columnNames = {"product_id", "user_id"}),
        indexes = @Index(name = "idx_product_favorite_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class ProductFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
