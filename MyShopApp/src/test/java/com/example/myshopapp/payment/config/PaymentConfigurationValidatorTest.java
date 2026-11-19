package com.example.myshopapp.payment.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 启用支付但配置不完整时必须在启动阶段失败，而不是等用户下单才报错。 */
class PaymentConfigurationValidatorTest {

    @Test
    void enabledAlipayWithoutKeyFailsStartup() {
        PaymentProperties.Alipay alipay = new PaymentProperties.Alipay(true,
                "https://gateway.test", "app-123", null, "", "", "https://shop.test/orders");
        PaymentProperties properties = new PaymentProperties("https://shop.test", false, alipay, null);
        assertThatThrownBy(() -> new PaymentConfigurationValidator(properties).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("私钥文件不可读");
    }

    @Test
    void disabledChannelsDoNotBlockStartup() {
        PaymentProperties.Alipay alipay = new PaymentProperties.Alipay(false,
                null, null, null, null, null, null);
        PaymentProperties properties = new PaymentProperties(null, false, alipay, null);
        assertThatCode(() -> new PaymentConfigurationValidator(properties).afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}
