package com.example.myshopapp.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 管理员操作审计日志：记录后台写操作的执行人与结果，便于事后追溯。
 */
@Getter
@Setter
@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long operatorId;

    @Column(length = 50)
    private String operatorName;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false)
    private String uri;

    @Column(length = 500)
    private String queryString;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private int status;

    @Column(nullable = false)
    private long durationMs;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
