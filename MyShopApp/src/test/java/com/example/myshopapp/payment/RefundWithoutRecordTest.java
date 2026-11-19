package com.example.myshopapp.payment;

import com.example.myshopapp.order.OrderService;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.order.repository.PaymentRecordRepository;
import com.example.myshopapp.payment.alipay.AlipayService;
import com.example.myshopapp.payment.config.PaymentProperties;
import com.example.myshopapp.payment.dto.RefundResult;
import com.example.myshopapp.payment.wechat.WechatPayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 缺少支付流水时必须拒绝自动退款，杜绝“状态已退款但渠道没退钱”。 */
@ExtendWith(MockitoExtension.class)
class RefundWithoutRecordTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentRecordRepository paymentRecordRepository;
    @Mock
    private AlipayService alipayService;
    @Mock
    private WechatPayService wechatPayService;

    @Test
    void noPaymentRecordIsRejected() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("OD20261118123456789001");
        when(paymentRecordRepository.findFirstByOrderIdOrderByIdDesc(1L))
                .thenReturn(Optional.empty());

        RefundResult result = service().refund(order, BigDecimal.TEN, "RF001", "售后退款");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("缺少支付流水");
    }

    private PaymentService service() {
        PaymentProperties properties = new PaymentProperties("https://shop.test", false, null, null);
        return new PaymentService(orderRepository, orderService, paymentRecordRepository,
                alipayService, wechatPayService, properties);
    }
}
