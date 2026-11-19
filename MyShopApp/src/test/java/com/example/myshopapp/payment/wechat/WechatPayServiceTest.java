package com.example.myshopapp.payment.wechat;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.payment.PaymentTestSupport;
import com.example.myshopapp.payment.config.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 微信支付 APIv3：请求签名、回调解密与配置校验。 */
class WechatPayServiceTest {

    private static final String API_V3_KEY = "12345678901234567890123456789012";

    @TempDir
    private Path tempDir;

    private KeyPair keyPair;
    private WechatPayService service;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = PaymentTestSupport.keyPair();
        Path privatePath = PaymentTestSupport.writePrivateKey(tempDir, keyPair);
        service = new WechatPayService(properties(privatePath, API_V3_KEY), new ObjectMapper());
    }

    @Test
    void matchesMerchantChecksBothIds() {
        assertThat(service.matchesMerchant("wx-app", "1900000001")).isTrue();
        assertThat(service.matchesMerchant("wx-app", "other")).isFalse();
    }

    @Test
    void authorizationHeaderIsSignedWithMerchantKey() throws Exception {
        String header = buildAuthorization("POST", "/v3/pay/transactions/native", "{}");
        assertThat(header).startsWith("WECHATPAY2-SHA256-RSA2048");
        Map<String, String> fields = parseHeader(header);
        String message = "POST\n/v3/pay/transactions/native\n"
                + fields.get("timestamp") + "\n" + fields.get("nonce_str") + "\n{}\n";
        assertThat(PaymentTestSupport.verify(message, fields.get("signature"),
                keyPair.getPublic())).isTrue();
    }

    @Test
    void decryptResourceRestoresCallbackPayload() throws Exception {
        String nonce = "abcdefghijkl";
        String associatedData = "transaction";
        String plain = "{\"out_trade_no\":\"SO1\"}";
        Method method = WechatPayService.class.getDeclaredMethod("decryptResource",
                String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        Object result = method.invoke(service, associatedData, nonce,
                encrypt(plain, nonce, associatedData), API_V3_KEY);
        assertThat(result).isEqualTo(plain);
    }

    @Test
    void rejectShortApiV3Key() throws Exception {
        PaymentProperties props = properties(tempDir.resolve("private.pem"), "short");
        WechatPayService target = new WechatPayService(props, new ObjectMapper());
        assertThatThrownBy(() -> target.createNativeOrder(new Order()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APIv3 密钥长度");
    }

    private PaymentProperties properties(Path privatePath, String apiV3Key) {
        PaymentProperties.Wechat wechat = new PaymentProperties.Wechat(true,
                "wx-app", "1900000001", "SERIAL123", privatePath.toString(),
                apiV3Key, tempDir.resolve("cert.pem").toString());
        return new PaymentProperties("https://shop.test", false, null, wechat);
    }

    private String buildAuthorization(String method, String path, String body) throws Exception {
        Method target = WechatPayService.class.getDeclaredMethod("buildAuthorization",
                String.class, String.class, String.class);
        target.setAccessible(true);
        return (String) target.invoke(service, method, path, body);
    }

    private Map<String, String> parseHeader(String header) {
        Map<String, String> fields = new HashMap<>();
        String payload = header.substring(header.indexOf(' ') + 1);
        for (String part : payload.split(",")) {
            String[] pair = part.trim().split("=", 2);
            fields.put(pair[0], pair[1].replace("\"", ""));
        }
        return fields;
    }

    private String encrypt(String plain, String nonce, String associatedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(API_V3_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }
}
