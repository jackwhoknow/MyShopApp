package com.example.myshopapp.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用支付配置绑定。
 */
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfig {
}
