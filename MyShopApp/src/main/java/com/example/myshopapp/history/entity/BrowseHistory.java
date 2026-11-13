package com.example.myshopapp.history.entity;

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

import java.time.LocalDateTime;

/**
 * 浏览历史：同一用户对同一商品只保留一条记录，重复浏览时刷新浏览时间。
 */
@Entity
@Table(name = "browse_history",
        uniqueConstraints = @UniqueConstraint(name = "uk_browse_history",
                columnNames = {"product_id", "user_id"}),
        indexes = @Index(name = "idx_browse_history_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class BrowseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime viewedAt;
}
