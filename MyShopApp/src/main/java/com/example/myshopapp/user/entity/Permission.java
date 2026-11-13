package com.example.myshopapp.user.entity;

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

/**
 * 权限点：菜单 / 按钮 / 接口，RBAC 的细粒度控制。
 */
@Entity
@Table(name = "sys_permission")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionType type;

    /** 父级权限，用于菜单树 */
    private Long parentId;

    /** 前端路由或接口路径 */
    @Column(length = 200)
    private String path;

    public Permission(String code, String name, PermissionType type, String path) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.path = path;
    }
}
