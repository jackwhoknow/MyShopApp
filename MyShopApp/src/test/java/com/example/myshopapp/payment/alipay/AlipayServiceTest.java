package com.example.myshopapp.payment.alipay;

import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.payment.PaymentTestSupport;
import com.example.myshopapp.payment.config.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 支付宝真实通道：下单签名与异步通知验签的正确性是资金安全底线。 */
class AlipayServiceTest {

    @TempDir
    private Path tempDir;

    private KeyPair keyPair;
    private PublicKey publicKey;
    private AlipayService service;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = PaymentTestSupport.keyPair();
        publicKey = keyPair.getPublic();
        Path privatePath = PaymentTestSupport.writePrivateKey(tempDir, keyPair);
        Path publicPath = PaymentTestSupport.writePublicKey(tempDir, keyPair);
        service = new AlipayService(properties(privatePath, publicPath), new ObjectMapper());
    }

    @Test
    void pagePayUrlContainsSignedParameters() throws Exception {
        String url = service.createPagePayUrl(order());
        Map<String, String> params = parseQuery(url);
        assertThat(params).containsEntry("app_id", "app-123")
                .containsEntry("method", "alipay.trade.page.pay");
        assertThat(PaymentTestSupport.verify(signContent(params, false),
                params.get("sign"), publicKey)).isTrue();
    }

    @Test
    void verifyNotifyAcceptsSignedCallback() throws Exception {
        Map<String, String> params = callbackParams();
        params.put("sign", PaymentTestSupport.sign(signContent(params, true), keyPair.getPrivate()));
        assertThatCode(() -> service.verifyNotify(params)).doesNotThrowAnyException();
    }

    @Test
    void verifyNotifyRejectsTamperedAmount() throws Exception {
        Map<String, String> params = callbackParams();
        params.put("sign", PaymentTestSupport.sign(signContent(params, true), keyPair.getPrivate()));
        params.put("total_amount", "0.01");
        assertThatThrownBy(() -> service.verifyNotify(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("签名校验失败");
    }

    private PaymentProperties properties(Path privatePath, Path publicPath) {
        PaymentProperties.Alipay alipay = new PaymentProperties.Alipay(true,
                "https://gateway.test", "app-123", "seller-123",
                privatePath.toString(), publicPath.toString(), "https://shop.test/orders");
        return new PaymentProperties("https://shop.test", false, alipay, null);
    }

    private Order order() {
        Order order = new Order();
        order.setOrderNo("SO202611180001");
        order.setTotalAmount(new BigDecimal("12.34"));
        return order;
    }

    private Map<String, String> callbackParams() {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", "app-123");
        params.put("seller_id", "seller-123");
        params.put("sign_type", "RSA2");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("out_trade_no", "SO202611180001");
        params.put("total_amount", "12.34");
        params.put("trade_no", "ALI123");
        return params;
    }

    private Map<String, String> parseQuery(String url) throws Exception {
        String query = url.substring(url.indexOf('?') + 1);
        Map<String, String> params = new HashMap<>();
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            params.put(pair[0], URLDecoder.decode(pair[1], "UTF-8"));
        }
        return params;
    }

    private String signContent(Map<String, String> params, boolean excludeSignType) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !(excludeSignType && "sign_type".equals(entry.getKey())))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}
