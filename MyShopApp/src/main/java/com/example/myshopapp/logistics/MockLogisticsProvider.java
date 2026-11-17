package com.example.myshopapp.logistics;

import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模拟物流：不依赖第三方接口，发货后按固定节奏生成轨迹，
 * 便于本地开发与演示；配置 myshop.logistics.provider=kuaidi100 后由真实实现接管。
 */
@Component
@ConditionalOnProperty(prefix = "myshop.logistics", name = "provider",
        havingValue = "mock", matchIfMissing = true)
public class MockLogisticsProvider implements LogisticsProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public SubOrderLogisticsVO query(SubOrder subOrder) {
        List<SubOrderLogisticsVO.TraceVO> traces = new ArrayList<>();
        LocalDateTime shippedAt = subOrder.getShippedAt();
        if (shippedAt != null) {
            String company = StringUtils.hasText(subOrder.getShipCompany())
                    ? subOrder.getShipCompany() : "物流公司";
            LocalDateTime now = LocalDateTime.now();

            traces.add(new SubOrderLogisticsVO.TraceVO("已发货", "商家已发货，等待揽收", "商家仓库", shippedAt));

            if (subOrder.getStatus() == SubOrderStatus.COMPLETED) {
                LocalDateTime signedAt = subOrder.getCompletedAt() != null
                        ? subOrder.getCompletedAt() : now;
                long span = Math.max(Duration.between(shippedAt, signedAt).getSeconds(), 0);
                traces.add(new SubOrderLogisticsVO.TraceVO("已揽收",
                        company + "已揽收快件", "揽收网点", offset(shippedAt, span, 0.25)));
                traces.add(new SubOrderLogisticsVO.TraceVO("运输中",
                        "快件已发出，正在运往收货城市", "转运中心", offset(shippedAt, span, 0.55)));
                traces.add(new SubOrderLogisticsVO.TraceVO("派送中",
                        "快件已到达收货城市，正在派送", "收货城市配送站", offset(shippedAt, span, 0.85)));
                traces.add(new SubOrderLogisticsVO.TraceVO("已签收", "买家已确认收货", "收货地址", signedAt));
            } else {
                LocalDateTime pickupAt = shippedAt.plusHours(1);
                if (!now.isBefore(pickupAt)) {
                    traces.add(new SubOrderLogisticsVO.TraceVO("已揽收",
                            company + "已揽收快件", "揽收网点", pickupAt));
                }
                LocalDateTime transitAt = shippedAt.plusHours(6);
                if (!now.isBefore(transitAt)) {
                    traces.add(new SubOrderLogisticsVO.TraceVO("运输中",
                            "快件已发出，正在运往收货城市", "转运中心", transitAt));
                }
                LocalDateTime deliveringAt = shippedAt.plusHours(24);
                if (!now.isBefore(deliveringAt)) {
                    traces.add(new SubOrderLogisticsVO.TraceVO("派送中",
                            "快件已到达收货城市，正在派送", "收货城市配送站", deliveringAt));
                }
            }
            Collections.reverse(traces);
        }

        return new SubOrderLogisticsVO(
                subOrder.getId(),
                subOrder.getSubOrderNo(),
                subOrder.getStatus().name(),
                subOrder.getShipCompany(),
                subOrder.getTrackingNo(),
                subOrder.getShippedAt(),
                subOrder.getCompletedAt(),
                traces);
    }

    private static LocalDateTime offset(LocalDateTime start, long spanSeconds, double ratio) {
        return start.plusSeconds((long) (spanSeconds * ratio));
    }
}
