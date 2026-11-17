package com.example.myshopapp.order;

import com.example.myshopapp.order.entity.AfterSaleStatus;
import com.example.myshopapp.order.repository.AfterSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后超时自动关闭：待处理超过约定天数仍未处理的售后单由系统关闭，
 * 避免售后单长期占用子订单状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AfterSaleTimeoutTask {

    private final AfterSaleRepository afterSaleRepository;
    private final AfterSaleService afterSaleService;

    @Value("${myshop.order.after-sale-close-days:7}")
    private long closeDays;

    @Scheduled(fixedDelayString = "${myshop.order.after-sale-close-scan-ms:300000}",
            initialDelayString = "${myshop.order.after-sale-close-scan-ms:300000}")
    @SchedulerLock(name = "afterSaleTimeoutScan", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
    public void scanTimeoutAfterSales() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(closeDays);
        List<Long> ids = afterSaleRepository
                .findIdsByStatusAndCreatedAtBefore(AfterSaleStatus.PENDING, cutoff);
        if (ids.isEmpty()) {
            return;
        }

        int closed = 0;
        for (Long id : ids) {
            try {
                if (afterSaleService.autoCloseTimeout(id, cutoff)) {
                    closed++;
                }
            } catch (Exception e) {
                log.warn("售后单 {} 自动关闭失败：{}", id, e.getMessage());
            }
        }
        log.info("售后超时扫描完成：待处理 {} 单，成功关闭 {} 单", ids.size(), closed);
    }
}
