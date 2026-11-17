package com.example.myshopapp.common.config;

import com.example.myshopapp.user.RoleCode;
import com.example.myshopapp.user.entity.Permission;
import com.example.myshopapp.user.entity.PermissionType;
import com.example.myshopapp.user.entity.Role;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.repository.PermissionRepository;
import com.example.myshopapp.user.repository.RoleRepository;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 启动时初始化基础数据：三种角色、权限点、默认管理员。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        Permission productManage = ensurePermission("product:manage", "商品管理", "/api/merchant/products");
        Permission orderManage = ensurePermission("order:manage", "订单管理", "/api/merchant/orders");
        Permission shopAudit = ensurePermission("shop:audit", "店铺审核", "/api/admin/shops");
        Permission userManage = ensurePermission("user:manage", "用户管理", "/api/admin/users");

        Role buyer = ensureRole(RoleCode.BUYER, "买家");
        Role merchant = ensureRole(RoleCode.MERCHANT, "商家");
        Role admin = ensureRole(RoleCode.ADMIN, "平台管理员");

        merchant.getPermissions().add(productManage);
        merchant.getPermissions().add(orderManage);
        admin.getPermissions().add(productManage);
        admin.getPermissions().add(orderManage);
        admin.getPermissions().add(shopAudit);
        admin.getPermissions().add(userManage);
        roleRepository.save(merchant);
        roleRepository.save(admin);

        ensureAdmin(admin);

        log.info("基础数据初始化完成：买家={}，商家={}，管理员={}", buyer.getCode(), merchant.getCode(), admin.getCode());
    }

    /**
     * 默认管理员：账号不存在且配置了 ADMIN_PASSWORD 时才创建，不再内置 admin123 弱密码。
     */
    private void ensureAdmin(Role admin) {
        String username = environment.getProperty("myshop.init.admin-username", "admin");
        if (userRepository.existsByUsername(username)) {
            log.info("管理员账号已存在，跳过初始化：{}", username);
            return;
        }
        String initialPassword = environment.getProperty("myshop.init.admin-password", "");
        if (!StringUtils.hasText(initialPassword)) {
            log.warn("未配置 ADMIN_PASSWORD，跳过默认管理员创建；首次部署请通过环境变量提供强密码");
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setNickname("平台管理员");
        user.getRoles().add(admin);
        userRepository.save(user);
        log.info("已根据 ADMIN_PASSWORD 配置创建管理员：{}", username);
    }

    private Role ensureRole(String code, String name) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> roleRepository.save(new Role(code, name)));
    }

    private Permission ensurePermission(String code, String name, String path) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new Permission(code, name, PermissionType.BUTTON, path)));
    }
}
