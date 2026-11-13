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
 * 平台公告：置顶公告排在最前，其余按发布时间倒序。
 */
@Entity
@Table(name = "announcement")
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime publishedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
