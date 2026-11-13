package com.example.myshopapp.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 店铺关注关系：同一用户对同一店铺只会有一条记录。
 */
@Entity
@Table(name = "shop_follow", uniqueConstraints =
        @UniqueConstraint(name = "uk_shop_follow", columnNames = {"user_id", "shop_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ShopFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
