package com.example.myshopapp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 安全相关可配置参数，允许通过环境变量覆盖。
 */
@ConfigurationProperties(prefix = "myshop.security")
public record SecurityProperties(
        List<String> corsAllowedOrigins,
        int maxLoginFailures,
        int lockMinutes,
        int loginRateLimit,
        int loginRateWindowSeconds) {

    public SecurityProperties {
        if (corsAllowedOrigins == null || corsAllowedOrigins.isEmpty()) {
            corsAllowedOrigins = List.of("http://localhost:5173");
        }
        if (maxLoginFailures <= 0) {
            maxLoginFailures = 5;
        }
        if (lockMinutes <= 0) {
            lockMinutes = 15;
        }
        if (loginRateLimit <= 0) {
            loginRateLimit = 20;
        }
        if (loginRateWindowSeconds <= 0) {
            loginRateWindowSeconds = 300;
        }
    }
}
