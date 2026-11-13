package com.example.myshopapp.notification.entity;

import com.example.myshopapp.user.entity.User;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 站内通知：一条消息属于一个用户，点击后按 link 跳转到对应页面。
 */
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_user_read", columnList = "user_id, read_flag")
})
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    /** 前端路由，点击通知后跳转，可为空表示纯文本消息 */
    @Column(length = 200)
    private String link;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
