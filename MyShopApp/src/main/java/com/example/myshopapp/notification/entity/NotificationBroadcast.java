package com.example.myshopapp.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 管理员群发记录：保存标题、内容、目标人群与实际发送人数，供后台查看发送历史。
 */
@Entity
@Table(name = "notification_broadcast")
@Getter
@Setter
@NoArgsConstructor
public class NotificationBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(length = 200)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationAudience audience;

    @Column(nullable = false)
    private int recipientCount;

    @Column(nullable = false, length = 50)
    private String senderName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
