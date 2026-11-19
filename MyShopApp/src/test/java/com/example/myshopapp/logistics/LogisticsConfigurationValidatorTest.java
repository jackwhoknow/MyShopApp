package com.example.myshopapp.logistics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 生产物流必须配置真实账号，未知渠道或未配置时拒绝启动。 */
class LogisticsConfigurationValidatorTest {

    @Test
    void kuaidi100WithoutCredentialsFailsStartup() {
        LogisticsProperties properties = new LogisticsProperties("kuaidi100",
                new LogisticsProperties.Kuaidi100(null, null, null));
        assertThatThrownBy(() -> validator(properties).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须配置");
    }

    @Test
    void unknownProviderFailsStartup() {
        LogisticsProperties properties = new LogisticsProperties("unknown", null);
        assertThatThrownBy(() -> validator(properties).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不支持的 provider");
    }

    @Test
    void configuredKuaidi100PassesStartup() {
        LogisticsProperties properties = new LogisticsProperties("kuaidi100",
                new LogisticsProperties.Kuaidi100("customer-1", "key-1", null));
        assertThatCode(() -> validator(properties).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void mockProviderOnlyForTests() {
        assertThatCode(() -> validator(new LogisticsProperties("mock", null)).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    private LogisticsConfigurationValidator validator(LogisticsProperties properties) {
        return new LogisticsConfigurationValidator(properties);
    }
}
