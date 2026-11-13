package com.example.myshopapp.payment;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.OrderService;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.PaymentRecord;
import com.example.myshopapp.order.entity.PaymentStatus;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.order.repository.PaymentRecordRepository;
import com.example.myshopapp.payment.alipay.AlipayService;
import com.example.myshopapp.payment.config.PaymentProperties;
import com.example.myshopapp.payment.dto.PaymentPrepareVO;
import com.example.myshopapp.payment.dto.PaymentStatusVO;
import com.example.myshopapp.payment.dto.RefundResult;
import com.example.myshopapp.payment.wechat.WechatPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 支付编排：按渠道生成支付参数，模拟支付直接走统一入账逻辑；
 * 退款按原支付渠道原路退回，渠道不可用时记录为待退款，留给对账任务重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentRecordRepository paymentRecordRepository;
    private final AlipayService alipayService;
    private final WechatPayService wechatPayService;
    private final PaymentProperties properties;

    public PaymentPrepareVO prepare(Long userId, Long orderId, String channel) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单当前状态不可支付");
        }

        PaymentChannel paymentChannel;
        try {
            paymentChannel = PaymentChannel.valueOf(
                    channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的支付渠道");
        }

        return switch (paymentChannel) {
            case MOCK -> mockPay(order);
            case ALIPAY -> new PaymentPrepareVO(PaymentChannel.ALIPAY.name(),
                    alipayService.createPagePayUrl(order), null, false);
            case WECHAT -> new PaymentPrepareVO(PaymentChannel.WECHAT.name(),
                    null, wechatPayService.createNativeOrder(order), false);
        };
    }

    public PaymentStatusVO status(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        boolean paid = order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.COMPLETED;
        return new PaymentStatusVO(order.getId(), order.getOrderNo(), order.getStatus().name(), paid);
    }

    /**
     * 售后退款：按订单最近一笔支付记录原路退回。
     * - 无支付记录或模拟支付：直接标记退款完成（本地联调可用）；
     * - 真实渠道：调用渠道退款接口，失败时记录为待退款，由对账任务重试，避免出现“售后已退款但钱没退”的情况。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundResult refund(Order order, BigDecimal amount, String refundNo, String reason) {
        PaymentRecord record = paymentRecordRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .orElse(null);
        if (record == null) {
            log.info("订单 {} 没有支付记录，按本地退款处理", order.getOrderNo());
            return RefundResult.ok(refundNo);
        }
        if (record.getStatus() == PaymentStatus.REFUNDED) {
            return RefundResult.ok(record.getRefundNo());
        }

        String channel = record.getChannel() == null ? "" : record.getChannel().toUpperCase(Locale.ROOT);
        RefundResult result;
        if (PaymentChannel.MOCK.name().equals(channel)) {
            result = RefundResult.ok(refundNo);
        } else if (PaymentChannel.ALIPAY.name().equals(channel)) {
            result = alipayService.isEnabled()
                    ? alipayService.refund(order.getOrderNo(), amount, refundNo, reason)
                    : RefundResult.fail("支付宝渠道未启用，无法自动退款");
        } else if (PaymentChannel.WECHAT.name().equals(channel)) {
            result = wechatPayService.isEnabled()
                    ? wechatPayService.refund(order, amount, refundNo, reason)
                    : RefundResult.fail("微信支付渠道未启用，无法自动退款");
        } else {
            result = RefundResult.fail("未知支付渠道：" + channel);
        }

        if (result.success()) {
            record.setStatus(PaymentStatus.REFUNDED);
            record.setRefundNo(result.refundNo());
            record.setRefundedAt(LocalDateTime.now());
        } else {
            // 渠道退款失败：保留待退款标记，由对账任务持续重试
            record.setStatus(PaymentStatus.REFUND_REQUIRED);
            log.warn("订单 {} 退款失败：{}", order.getOrderNo(), result.message());
        }
        paymentRecordRepository.save(record);
        return result;
    }

    /** 模拟支付只在本地/测试环境可用，避免生产环境绕过真实支付 */
    private PaymentPrepareVO mockPay(Order order) {
        if (!properties.mockEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "模拟支付未开启");
        }
        OrderService.SettlementResult result = orderService.settlePayment(
                order.getOrderNo(), order.getTotalAmount(), PaymentChannel.MOCK.name(),
                "MOCK" + order.getOrderNo());
        if (!result.accepted()) {
            throw new BusinessException(ErrorCode.CONFLICT, result.message());
        }
        return new PaymentPrepareVO(PaymentChannel.MOCK.name(), null, null, true);
    }
}
