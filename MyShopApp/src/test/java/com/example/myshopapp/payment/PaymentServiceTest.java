package com.example.myshopapp.payment;

import com.example.myshopapp.order.OrderService;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.order.repository.PaymentRecordRepository;
import com.example.myshopapp.payment.alipay.AlipayService;
import com.example.myshopapp.payment.config.PaymentProperties;
import com.example.myshopapp.payment.wechat.WechatPayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** 前端渠道列表必须与实际启用的支付通道一致。 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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
    void availableChannelsReflectConfiguration() {
        assertThat(service(true, true, false).availableChannels())
                .containsExactly("MOCK", "ALIPAY");
        assertThat(service(false, false, true).availableChannels())
                .containsExactly("WECHAT");
        assertThat(service(false, false, false).availableChannels()).isEmpty();
    }

    private PaymentService service(boolean mock, boolean alipay, boolean wechat) {
        PaymentProperties properties = new PaymentProperties("https://shop.test", mock,
                new PaymentProperties.Alipay(alipay, null, null, null, null, null, null),
                new PaymentProperties.Wechat(wechat, null, null, null, null, null, null));
        return new PaymentService(orderRepository, orderService, paymentRecordRepository,
                alipayService, wechatPayService, properties);
    }
}
