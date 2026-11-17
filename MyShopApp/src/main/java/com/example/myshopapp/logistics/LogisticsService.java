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

    public LogisticsService(List<LogisticsProvider> providers) {
        // 有多个实现时优先选择非 mock 的真实实现
        this.provider = providers.stream()
                .filter(item -> !"mock".equals(item.name()))
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("缺少物流 Provider 实现")));
        log.info("物流 Provider 已启用：{}", provider.name());
    }

    public SubOrderLogisticsVO query(SubOrder subOrder) {
        return provider.query(subOrder);
    }
}
