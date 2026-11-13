package com.example.myshopapp.coupon.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 买家领取的券：每人每张券只能领一次，使用时记录订单号。
 */
@Entity
@Table(name = "user_coupon",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_coupon", columnNames = {"coupon_id", "user_id"}),
        indexes = @Index(name = "idx_user_coupon_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserCouponStatus status = UserCouponStatus.UNUSED;

    /** 使用该券的订单 id，未使用时为空 */
    private Long orderId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime claimedAt;

    private LocalDateTime usedAt;
}
