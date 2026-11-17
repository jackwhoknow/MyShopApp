package com.example.myshopapp.order;

import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动确认收货：已发货超过约定天数仍未确认收货的子订单由系统自动完成，
 * 避免订单长期停留在已发货状态影响结算与售后时效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteTask {

    private final SubOrderRepository subOrderRepository;
    private final OrderService orderService;

    @Value("${myshop.order.auto-complete-days:7}")
    private long autoCompleteDays;

    @Scheduled(fixedDelayString = "${myshop.order.auto-complete-scan-ms:300000}",
            initialDelayString = "${myshop.order.auto-complete-scan-ms:300000}")
    @SchedulerLock(name = "orderAutoCompleteScan", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
    public void scanAutoCompleteOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(autoCompleteDays);
        List<Long> subOrderIds = subOrderRepository
                .findIdsByStatusAndShippedAtBefore(SubOrderStatus.SHIPPED, cutoff);
        if (subOrderIds.isEmpty()) {
            return;
        }

        int completed = 0;
        for (Long subOrderId : subOrderIds) {
            try {
                if (orderService.autoCompleteSubOrder(subOrderId, cutoff)) {
                    completed++;
                }
            } catch (Exception e) {
                log.warn("子订单 {} 自动确认收货失败：{}", subOrderId, e.getMessage());
            }
        }
        log.info("自动确认收货扫描完成：待处理 {} 单，成功完成 {} 单", subOrderIds.size(), completed);
    }
}
