package com.example.myshopapp.order.dto;

import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 子订单物流轨迹：基于发货时间与完成时间生成模拟轨迹，供买家与商家查看。
 */
public record SubOrderLogisticsVO(
        Long subOrderId,
        String subOrderNo,
        String status,
        String shipCompany,
        String trackingNo,
        LocalDateTime shippedAt,
        LocalDateTime completedAt,
        List<TraceVO> traces
) {

    public record TraceVO(
            String title,
            String description,
            String location,
            LocalDateTime occurredAt
    ) {
    }

    /**
     * 模拟节点：
     * 已发货后按固定时间推进（1 小时揽收、6 小时运输、24 小时派送），只展示已到达的节点；
     * 订单完成后按发货到签收的时间比例补齐全程，最新节点排在最前。
     */
    public static SubOrderLogisticsVO from(SubOrder subOrder) {
        List<TraceVO> traces = new ArrayList<>();
        LocalDateTime shippedAt = subOrder.getShippedAt();
        if (shippedAt != null) {
            String company = StringUtils.hasText(subOrder.getShipCompany())
                    ? subOrder.getShipCompany() : "物流公司";
            LocalDateTime now = LocalDateTime.now();

            traces.add(new TraceVO("已发货", "商家已发货，等待揽收", "商家仓库", shippedAt));

            if (subOrder.getStatus() == SubOrderStatus.COMPLETED) {
                LocalDateTime signedAt = subOrder.getCompletedAt() != null
                        ? subOrder.getCompletedAt() : now;
                long span = Math.max(Duration.between(shippedAt, signedAt).getSeconds(), 0);
                traces.add(new TraceVO("已揽收", company + "已揽收快件", "揽收网点", offset(shippedAt, span, 0.25)));
                traces.add(new TraceVO("运输中", "快件已发出，正在运往收货城市", "转运中心", offset(shippedAt, span, 0.55)));
                traces.add(new TraceVO("派送中", "快件已到达收货城市，正在派送", "收货城市配送站", offset(shippedAt, span, 0.85)));
                traces.add(new TraceVO("已签收", "买家已确认收货", "收货地址", signedAt));
            } else {
                LocalDateTime pickupAt = shippedAt.plusHours(1);
                if (!now.isBefore(pickupAt)) {
                    traces.add(new TraceVO("已揽收", company + "已揽收快件", "揽收网点", pickupAt));
                }
                LocalDateTime transitAt = shippedAt.plusHours(6);
                if (!now.isBefore(transitAt)) {
                    traces.add(new TraceVO("运输中", "快件已发出，正在运往收货城市", "转运中心", transitAt));
                }
                LocalDateTime deliveringAt = shippedAt.plusHours(24);
                if (!now.isBefore(deliveringAt)) {
                    traces.add(new TraceVO("派送中", "快件已到达收货城市，正在派送", "收货城市配送站", deliveringAt));
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
