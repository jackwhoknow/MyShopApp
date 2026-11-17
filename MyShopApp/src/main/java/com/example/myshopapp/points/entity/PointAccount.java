package com.example.myshopapp.points.entity;

import com.example.myshopapp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 买家积分账户：懒创建，余额允许为负（整单退款需扣回已赠送积分）。
 */
@Entity
@Table(name = "user_point_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_point_account_user", columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class PointAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int balance = 0;

    /** 累计获得积分 */
    @Column(nullable = false)
    private int totalEarned = 0;

    /** 累计使用积分 */
    @Column(nullable = false)
    private int totalSpent = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
