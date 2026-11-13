package com.example.myshopapp.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水：模拟支付成功也留痕，方便后续对接真实支付渠道。
 */
@Entity
@Table(name = "payment_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_pay_no", columnNames = "pay_no"),
        indexes = @Index(name = "idx_pay_order", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String payNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.SUCCESS;

    private LocalDateTime paidAt;

    /** 第三方退款单号，退款成功后回填 */
    @Column(length = 64)
    private String refundNo;

    private LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
