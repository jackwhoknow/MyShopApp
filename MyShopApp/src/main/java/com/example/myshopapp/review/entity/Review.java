package com.example.myshopapp.review.entity;

import com.example.myshopapp.order.entity.OrderItem;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 商品评价：订单完成后买家对订单商品评价，一个订单商品只能评价一次。
 * 商家可对评价回复一次。
 */
@Entity
@Table(name = "review",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_order_item", columnNames = "order_item_id"),
        indexes = {
                @Index(name = "idx_review_shop", columnList = "shop_id"),
                @Index(name = "idx_review_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 被评价的订单商品，一个订单商品只能有一条评价 */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** 评价买家 */
    @Column(nullable = false)
    private Long userId;

    /** 评价所属店铺，商家按店铺查询 */
    @Column(nullable = false)
    private Long shopId;

    @Column(nullable = false)
    private Long productId;

    /** 星级 1-5 */
    @Column(nullable = false)
    private int rating;

    @Column(length = 500)
    private String content;

    /** 评价图片，多张以英文逗号分隔 */
    @Column(length = 1000)
    private String images;

    /** 审核状态：命中敏感词待审，其余自动通过 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewStatus status = ReviewStatus.APPROVED;

    /** 审核备注，驳回时填写原因 */
    @Column(length = 200)
    private String auditRemark;

    /** 商家回复，只允许回复一次 */
    @Column(length = 500)
    private String reply;

    private LocalDateTime repliedAt;

    /** 追评内容，只允许追加一次 */
    @Column(length = 500)
    private String appendContent;

    /** 追评图片，多张以英文逗号分隔 */
    @Column(length = 1000)
    private String appendImages;

    private LocalDateTime appendedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
