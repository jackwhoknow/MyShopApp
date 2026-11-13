package com.example.myshopapp.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付配置：所有密钥只从环境变量/服务端配置文件读取，绝不返回给前端。
 */
@ConfigurationProperties(prefix = "myshop.payment")
public record PaymentProperties(
        /** 支付平台回调本站的公网地址前缀，例如 https://shop.example.com */
        String notifyBaseUrl,
        /** 本地/测试环境是否允许模拟支付，生产环境必须为 false */
        boolean mockEnabled,
        Alipay alipay,
        Wechat wechat) {

    public record Alipay(
            boolean enabled,
            String serverUrl,
            String appId,
            /** 可选：支付宝账号 PID，配置后回调会额外校验 */
            String sellerId,
            /** 应用私钥 PKCS8 PEM 文件路径 */
            String privateKeyPath,
            /** 支付宝公钥文件路径（支持公钥或证书 PEM） */
            String alipayPublicKeyPath,
            /** 支付完成后浏览器跳回的页面地址 */
            String returnUrl) {
    }

    public record Wechat(
            boolean enabled,
            String appId,
            String mchId,
            /** 商户 API 证书序列号 */
            String serialNo,
            /** 商户 API 私钥 PKCS8 PEM 文件路径 */
            String privateKeyPath,
            /** APIv3 密钥，32 位，用于回调报文解密 */
            String apiV3Key,
            /** 微信支付平台证书 PEM 文件路径，用于回调验签 */
            String platformCertPath) {
    }
}
