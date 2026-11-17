package com.example.myshopapp.logistics;

import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟物流轨迹：发货后按时间推进生成节点，最新节点排在最前。
 */
class MockLogisticsProviderTest {

    private final MockLogisticsProvider provider = new MockLogisticsProvider();

    @Test
    @DisplayName("刚发货只有发货节点")
    void onlyShippedTraceForFreshShipment() {
        SubOrder subOrder = shippedOrder(SubOrderStatus.SHIPPED, LocalDateTime.now().minusMinutes(5), null);

        SubOrderLogisticsVO result = provider.query(subOrder);

        assertThat(result.traces()).hasSize(1);
        assertThat(result.traces().get(0).title()).isEqualTo("已发货");
        assertThat(result.shipCompany()).isEqualTo("SF");
        assertThat(result.trackingNo()).isEqualTo("SF123");
    }

    @Test
    @DisplayName("发货超过一天后出现派送节点，且按时间倒序排列")
    void deliveringTraceAfterOneDay() {
        SubOrder subOrder = shippedOrder(SubOrderStatus.SHIPPED,
                LocalDateTime.now().minusHours(30), null);

        SubOrderLogisticsVO result = provider.query(subOrder);

        assertThat(result.traces()).extracting(SubOrderLogisticsVO.TraceVO::title)
                .containsExactly("派送中", "运输中", "已揽收", "已发货");
    }

    @Test
    @DisplayName("已完成订单包含签收节点，签收时间取完成时间")
    void completedOrderHasSignedTrace() {
        LocalDateTime shippedAt = LocalDateTime.now().minusDays(2);
        LocalDateTime completedAt = LocalDateTime.now().minusDays(1);
        SubOrder subOrder = shippedOrder(SubOrderStatus.COMPLETED, shippedAt, completedAt);

        SubOrderLogisticsVO result = provider.query(subOrder);

        assertThat(result.traces()).extracting(SubOrderLogisticsVO.TraceVO::title)
                .containsExactly("已签收", "派送中", "运输中", "已揽收", "已发货");
        assertThat(result.traces().get(0).occurredAt()).isEqualTo(completedAt);
        assertThat(result.completedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("未发货订单没有轨迹")
    void noTraceBeforeShipment() {
        SubOrder subOrder = new SubOrder();
        subOrder.setId(1L);
        subOrder.setSubOrderNo("OD1-1");
        subOrder.setStatus(SubOrderStatus.WAIT_SHIP);

        SubOrderLogisticsVO result = provider.query(subOrder);

        assertThat(result.traces()).isEmpty();
        assertThat(result.status()).isEqualTo("WAIT_SHIP");
    }

    private SubOrder shippedOrder(SubOrderStatus status, LocalDateTime shippedAt, LocalDateTime completedAt) {
        SubOrder subOrder = new SubOrder();
        subOrder.setId(1L);
        subOrder.setSubOrderNo("OD1-1");
        subOrder.setStatus(status);
        subOrder.setShipCompany("SF");
        subOrder.setTrackingNo("SF123");
        subOrder.setShippedAt(shippedAt);
        subOrder.setCompletedAt(completedAt);
        return subOrder;
    }
}
