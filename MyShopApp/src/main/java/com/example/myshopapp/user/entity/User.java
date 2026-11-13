package com.example.myshopapp.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户表：买家、商家、平台管理员共用一张表，通过角色区分身份。
 */
@Entity
@Table(name = "sys_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt 加密后的密码，禁止明文存储 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /** 连续登录失败次数，登录成功后清零 */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /** 账号锁定截止时间，为空表示未锁定 */
    private LocalDateTime lockedUntil;

    /** 登录鉴权时需要角色，这里用 EAGER 避免懒加载问题 */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sys_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted = false;
}
