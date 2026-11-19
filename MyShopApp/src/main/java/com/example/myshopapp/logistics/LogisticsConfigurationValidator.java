package com.example.myshopapp.logistics;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 物流配置启动校验：启用真实查询却缺少账号，或配置了未知渠道时直接拒绝启动，
 * 避免生产环境把“查不到轨迹”误当成“没有轨迹”静默放行。
 */
@Component
public class LogisticsConfigurationValidator implements InitializingBean {

    private final LogisticsProperties properties;

    public LogisticsConfigurationValidator(LogisticsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        String provider = properties.provider();
        if ("mock".equalsIgnoreCase(provider)) {
            return;
        }
        if (!"kuaidi100".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("物流配置错误：不支持的 provider " + provider);
        }
        if (!properties.kuaidi100().configured()) {
            throw new IllegalStateException(
                    "物流配置错误：provider=kuaidi100 时必须配置 KUAIDI100_CUSTOMER 与 KUAIDI100_KEY");
        }
    }
}
