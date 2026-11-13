package com.example.myshopapp.payment;

import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.PaymentRecord;
import com.example.myshopapp.order.entity.PaymentStatus;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.order.repository.PaymentRecordRepository;
import com.example.myshopapp.payment.alipay.AlipayService;
import com.example.myshopapp.payment.config.PaymentProperties;
import com.example.myshopapp.payment.dto.RefundResult;
import com.example.myshopapp.payment.wechat.WechatPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 支付对账任务：
 * 1. 重试“钱已到账但订单已关闭”的待退款记录；
 * 2. 核对近期成功支付的记录，渠道侧实际未支付时标记为失败并记录告警日志。
 * 未配置真实渠道时自动跳过对应的查询。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationTask {

    /** 渠道侧确认已收款的状态 */
    private static final Set<String> ALIPAY_PAID = Set.of("TRADE_SUCCESS", "TRADE_FINISHED");
    private static final Set<String> WECHAT_PAID = Set.of("SUCCESS", "REFUND");

    private final PaymentRecordRepository paymentRecordRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final AlipayService alipayService;
    private final WechatPayService wechatPayService;
    private final PaymentProperties properties;

    @Scheduled(fixedDelayString = "${myshop.payment.reconcile-delay-ms:3600000}",
            initialDelayString = "${myshop.payment.reconcile-initial-delay-ms:300000}")
    @SchedulerLock(name = "paymentReconciliation", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void reconcile() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        retryRefunds(since);
        verifySuccessRecords(since);
    }

    /** 待退款记录重试：退款单号固定为 RF+支付单号，渠道侧幂等，不会重复退款 */
    private void retryRefunds(LocalDateTime since) {
        List<PaymentRecord> records = paymentRecordRepository
                .findTop50ByStatusAndCreatedAtAfterOrderByIdAsc(PaymentStatus.REFUND_REQUIRED, since);
        int succeeded = 0;
        for (PaymentRecord record : records) {
            Order order = orderRepository.findById(record.getOrderId()).orElse(null);
            if (order == null) {
                continue;
            }
            try {
                RefundResult result = paymentService.refund(order, record.getAmount(),
                        "RF" + record.getPayNo(), "系统自动退款重试");
                if (result.success()) {
                    succeeded++;
                }
            } catch (Exception e) {
                log.warn("支付单 {} 自动退款异常：{}", record.getPayNo(), e.getMessage());
            }
        }
        if (!records.isEmpty()) {
            log.info("支付对账：待退款 {} 笔，本次成功 {} 笔", records.size(), succeeded);
        }
    }

    /** 成功支付记录核对：渠道查询明确显示未支付时，标记失败并告警 */
    private void verifySuccessRecords(LocalDateTime since) {
        if (properties.mockEnabled()) {
            return;
        }
        List<PaymentRecord> records = paymentRecordRepository
                .findTop50ByStatusAndCreatedAtAfterOrderByIdAsc(PaymentStatus.SUCCESS, since);
        for (PaymentRecord record : records) {
            String channel = record.getChannel() == null ? "" : record.getChannel().toUpperCase(Locale.ROOT);
            Order order = orderRepository.findById(record.getOrderId()).orElse(null);
            if (order == null) {
                continue;
            }
            String state;
            try {
                if (PaymentChannel.ALIPAY.name().equals(channel) && alipayService.isEnabled()) {
                    state = alipayService.queryTradeState(order.getOrderNo());
                    if (state != null && !ALIPAY_PAID.contains(state)) {
                        markMismatch(record, state);
                    }
                } else if (PaymentChannel.WECHAT.name().equals(channel) && wechatPayService.isEnabled()) {
                    state = wechatPayService.queryTradeState(order.getOrderNo());
                    if (state != null && !WECHAT_PAID.contains(state)) {
                        markMismatch(record, state);
                    }
                }
            } catch (Exception e) {
                log.warn("支付单 {} 对账查询失败：{}", record.getPayNo(), e.getMessage());
            }
        }
    }

    private void markMismatch(PaymentRecord record, String state) {
        // 渠道侧未收款而本地记为成功，属于严重数据不一致，保留日志供人工核查
        log.error("支付对账不一致：支付单 {} 订单 {} 渠道状态 {}，请人工核查",
                record.getPayNo(), record.getOrderId(), state);
        record.setStatus(PaymentStatus.FAILED);
        paymentRecordRepository.save(record);
    }
}
