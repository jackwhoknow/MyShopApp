package com.example.myshopapp.support.entity;

import com.example.myshopapp.shop.entity.Shop;
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
 * 客服会话：买家与店铺一对一，会话列表展示最后一条消息与双方未读数。
 */
@Entity
@Table(name = "support_conversation",
        uniqueConstraints = @UniqueConstraint(name = "uk_support_conversation",
                columnNames = {"user_id", "shop_id"}),
        indexes = @Index(name = "idx_support_conversation_shop", columnList = "shop_id"))
@Getter
@Setter
@NoArgsConstructor
public class SupportConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    /** 关联订单，从订单页发起咨询时记录 */
    private Long orderId;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    private LocalDateTime lastMessageAt;

    @Column(length = 200)
    private String lastMessagePreview;

    @Column(nullable = false)
    private int userUnread = 0;

    @Column(nullable = false)
    private int shopUnread = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
