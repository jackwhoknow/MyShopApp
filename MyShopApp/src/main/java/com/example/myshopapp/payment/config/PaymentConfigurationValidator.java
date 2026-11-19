package com.example.myshopapp.payment.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 支付配置启动校验：渠道标记为启用但密钥或回调地址不完整时直接拒绝启动，
 * 避免用户下单后才发现支付不可用，也避免把配置错误暴露成线上 500。
 */
@Component
public class PaymentConfigurationValidator implements InitializingBean {

    private final PaymentProperties properties;

    public PaymentConfigurationValidator(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.alipay() != null && properties.alipay().enabled()) {
            validateAlipay();
        }
        if (properties.wechat() != null && properties.wechat().enabled()) {
            validateWechat();
        }
    }

    private void validateAlipay() {
        PaymentProperties.Alipay config = properties.alipay();
        require(StringUtils.hasText(config.appId()), "支付宝缺少 APP_ID");
        require(readable(config.privateKeyPath()), "支付宝应用私钥文件不可读");
        require(readable(config.alipayPublicKeyPath()), "支付宝公钥文件不可读");
        require(StringUtils.hasText(properties.notifyBaseUrl()), "支付回调地址未配置");
    }

    private void validateWechat() {
        PaymentProperties.Wechat config = properties.wechat();
        require(StringUtils.hasText(config.appId()), "微信支付缺少 APP_ID");
        require(StringUtils.hasText(config.mchId()), "微信支付缺少商户号");
        require(StringUtils.hasText(config.serialNo()), "微信支付缺少证书序列号");
        require(readable(config.privateKeyPath()), "微信支付商户私钥文件不可读");
        require(readable(config.platformCertPath()), "微信支付平台证书文件不可读");
        require(config.apiV3Key() != null && config.apiV3Key().length() == 32,
                "微信支付 APIv3 密钥必须为 32 位");
        require(StringUtils.hasText(properties.notifyBaseUrl()), "支付回调地址未配置");
    }

    private boolean readable(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        try {
            return Files.isReadable(Path.of(path));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalStateException("支付配置错误：" + message);
        }
    }
}
