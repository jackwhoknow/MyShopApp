package com.example.myshopapp.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 首页轮播图：由平台管理员维护，sortOrder 越小越靠前。
 */
@Entity
@Table(name = "banner")
@Getter
@Setter
@NoArgsConstructor
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    /** 图片地址，为空时前台用渐变色占位 */
    @Column(length = 500)
    private String imageUrl;

    /** 点击跳转地址，为空则不可点击 */
    @Column(length = 500)
    private String linkUrl;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
