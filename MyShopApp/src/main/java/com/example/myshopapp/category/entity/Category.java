package com.example.myshopapp.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 商品分类：支持无限级树。
 *
 * <p>level 表示层级（从 1 开始），path 保存从根到自身的 id 链，例如 "1/5/9"。
 * 查某个分类的整棵子树时只需 path = '1/5' 或 path LIKE '1/5/%'。
 */
@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_parent", columnList = "parent_id"),
        @Index(name = "idx_category_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 父分类 id，根分类为 null */
    private Long parentId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int level = 1;

    /** 从根到自身的 id 路径，如 1/5/9 */
    @Column(nullable = false, length = 200)
    private String path;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
