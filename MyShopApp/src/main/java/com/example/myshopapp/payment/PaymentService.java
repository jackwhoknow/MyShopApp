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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    /** 返回当前已配置且可用的支付渠道，前端据此只展示可支付的选项。 */
    public List<String> availableChannels() {
        List<String> channels = new ArrayList<>();
        if (properties.mockEnabled()) {
            channels.add(PaymentChannel.MOCK.name());
        }
        if (enabled(properties.alipay())) {
            channels.add(PaymentChannel.ALIPAY.name());
        }
        if (enabled(properties.wechat())) {
            channels.add(PaymentChannel.WECHAT.name());
        }
        return channels;
    }

    private boolean enabled(PaymentProperties.Alipay config) {
        return config != null && config.enabled();
    }

    private boolean enabled(PaymentProperties.Wechat config) {
        return config != null && config.enabled();
    }

    public PaymentStatusVO status(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        boolean paid = order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.COMPLETED;
        return new PaymentStatusVO(order.getId(), order.getOrderNo(), order.getStatus().name(), paid);
    }

    /**
     * 售后退款：按订单最近一笔支付记录原路退回。
     * 无支付记录、未知渠道或渠道未启用时一律失败，交由售后流程转人工处理，
     * 绝不把“本地标记退款”当成真实退款，避免出现钱没退但订单已结束。
     */
    @Transactional
    public RefundResult refund(Order order, BigDecimal amount, String refundNo, String reason) {
        PaymentRecord record = paymentRecordRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .orElse(null);
        if (record == null) {
            log.error("订单 {} 缺少支付流水，拒绝自动退款，需人工处理", order.getOrderNo());
            return RefundResult.fail("订单缺少支付流水，无法原路退款，请联系客服处理");
        }
        if (record.getStatus() == PaymentStatus.REFUNDED) {
            return RefundResult.ok(record.getRefundNo());
        }

        String channel = record.getChannel() == null ? "" : record.getChannel().toUpperCase(Locale.ROOT);
        RefundResult result = refundByChannel(order, amount, refundNo, reason, record, channel);

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

    /** 按原支付渠道退款；模拟支付退款也必须在显式开启的环境下才允许 */
    private RefundResult refundByChannel(Order order, BigDecimal amount, String refundNo,
                                         String reason, PaymentRecord record, String channel) {
        if (PaymentChannel.MOCK.name().equals(channel)) {
            return properties.mockEnabled()
                    ? RefundResult.ok(refundNo)
                    : RefundResult.fail("模拟支付已禁用，无法退款");
        }
        if (PaymentChannel.ALIPAY.name().equals(channel)) {
            return alipayService.isEnabled()
                    ? alipayService.refund(order.getOrderNo(), amount, refundNo, reason)
                    : RefundResult.fail("支付宝渠道未启用，无法自动退款");
        }
        if (PaymentChannel.WECHAT.name().equals(channel)) {
            return wechatPayService.isEnabled()
                    ? wechatPayService.refund(order, amount, refundNo, reason)
                    : RefundResult.fail("微信支付渠道未启用，无法自动退款");
        }
        return RefundResult.fail("未知支付渠道：" + channel);
    }

    /** 模拟支付只在测试 profile 可用，避免生产环境绕过真实支付 */
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
