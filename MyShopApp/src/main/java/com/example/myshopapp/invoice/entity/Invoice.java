package com.example.myshopapp.invoice.entity;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单发票申请：买家提交抬头信息，平台开具后状态变为 ISSUED。
 */
@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    /** PERSONAL 个人 / COMPANY 单位 */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 50)
    private String taxNo;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private BigDecimal amount;

    /** PENDING 待开票 / ISSUED 已开票 */
    @Column(nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime issuedAt;
}
