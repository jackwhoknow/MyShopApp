package com.example.myshopapp.logistics;

import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.SubOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物流查询门面：按配置选择 Provider，当前默认 Mock；
 * 接入快递 100 等服务时新增 Provider 并调整配置即可。
 */
@Slf4j
@Service
public class LogisticsService {

    private final LogisticsProvider provider;

    public LogisticsService(List<LogisticsProvider> providers, LogisticsProperties properties) {
        // 严格按配置选路：配置的 Provider 缺失时启动失败，绝不回退到模拟实现
        String configured = properties.provider();
        this.provider = providers.stream()
                .filter(item -> configured.equalsIgnoreCase(item.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "缺少物流 Provider 实现：" + configured));
        log.info("物流 Provider 已启用：{}", provider.name());
    }

    public SubOrderLogisticsVO query(SubOrder subOrder) {
        return provider.query(subOrder);
    }
}
