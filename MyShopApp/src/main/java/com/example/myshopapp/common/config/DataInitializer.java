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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

        if (!userRepository.existsByUsername("admin")) {
            User user = new User();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("admin123"));
            user.setNickname("平台管理员");
            user.getRoles().add(admin);
            userRepository.save(user);
            log.info("已创建默认管理员：admin / admin123");
        }

        log.info("基础数据初始化完成：买家={}，商家={}，管理员={}", buyer.getCode(), merchant.getCode(), admin.getCode());
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
