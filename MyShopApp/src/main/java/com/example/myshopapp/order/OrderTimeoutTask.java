package com.example.myshopapp.order;

import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消：扫描超过支付时限仍待支付的订单，逐单取消并回补库存、退回优惠券。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /** 支付时限（分钟），默认 30 分钟 */
    @Value("${myshop.order.timeout-minutes:30}")
    private long timeoutMinutes;

    @Scheduled(fixedDelayString = "${myshop.order.timeout-scan-ms:60000}",
            initialDelayString = "${myshop.order.timeout-scan-ms:60000}")
    @SchedulerLock(name = "orderTimeoutScan", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void scanTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Long> orderIds = orderRepository.findIdsByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        if (orderIds.isEmpty()) {
            return;
        }

        int cancelled = 0;
        for (Long orderId : orderIds) {
            try {
                if (orderService.cancelTimeoutOrder(orderId, cutoff)) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.warn("订单 {} 超时取消失败：{}", orderId, e.getMessage());
            }
        }
        log.info("订单超时扫描完成：待处理 {} 单，成功取消 {} 单", orderIds.size(), cancelled);
    }
}
