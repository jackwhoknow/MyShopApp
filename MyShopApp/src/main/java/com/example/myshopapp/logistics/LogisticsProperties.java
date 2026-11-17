package com.example.myshopapp.logistics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 物流查询配置：provider=mock 使用本地模拟，provider=kuaidi100 启用真实查询。
 */
@ConfigurationProperties(prefix = "myshop.logistics")
public record LogisticsProperties(String provider, Kuaidi100 kuaidi100) {

    public LogisticsProperties {
        provider = StringUtils.hasText(provider) ? provider : "mock";
        kuaidi100 = kuaidi100 == null ? new Kuaidi100(null, null, null) : kuaidi100;
    }

    public record Kuaidi100(String customer, String key, String endpoint) {

        public Kuaidi100 {
            endpoint = StringUtils.hasText(endpoint)
                    ? endpoint : "https://poll.kuaidi100.com/poll/query.do";
        }

        public boolean configured() {
            return StringUtils.hasText(customer) && StringUtils.hasText(key);
        }
    }
}
