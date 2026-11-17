package com.example.myshopapp.settlement.entity;

import com.example.myshopapp.shop.entity.Shop;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家提现申请：金额在提交时即冻结，驳回后释放，打款后计入已提现。
 */
@Entity
@Table(name = "withdrawal_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_withdrawal_no", columnNames = "withdrawal_no"),
        indexes = {
                @Index(name = "idx_withdrawal_shop", columnList = "shop_id"),
                @Index(name = "idx_withdrawal_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String withdrawalNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** 收款户名 */
    @Column(nullable = false, length = 50)
    private String accountName;

    /** 收款账号 */
    @Column(nullable = false, length = 64)
    private String accountNo;

    /** 开户银行或收款渠道 */
    @Column(nullable = false, length = 100)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    /** 平台审核备注，驳回时必填 */
    @Column(length = 200)
    private String adminRemark;

    /** 审核时间，通过或驳回时写入 */
    private LocalDateTime reviewedAt;

    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
