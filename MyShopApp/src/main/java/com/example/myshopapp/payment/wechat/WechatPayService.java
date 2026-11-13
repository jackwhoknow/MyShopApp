package com.example.myshopapp.payment.wechat;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.payment.config.PaymentProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import com.example.myshopapp.payment.dto.RefundResult;
import com.example.myshopapp.payment.support.PemKeyLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付 APIv3 Native 下单：服务端签名请求微信接口，用户用微信扫码完成支付。
 * 回调必须通过平台证书验签 + APIv3 密钥解密后才允许入账。
 */
@Slf4j
@Service
public class WechatPayService {

    private static final String BASE_URL = "https://api.mch.weixin.qq.com";
    private static final String NATIVE_PATH = "/v3/pay/transactions/native";
    private static final String REFUND_PATH = "/v3/refund/domestic/refunds";
    private static final String ORDER_QUERY_PREFIX = "/v3/pay/transactions/out-trade-no/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    /** 回调时间戳允许的最大偏差，防重放 */
    private static final long MAX_CLOCK_SKEW_SECONDS = 300;

    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private PrivateKey privateKey;
    private X509Certificate platformCertificate;

    public WechatPayService(PaymentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return properties.wechat() != null && properties.wechat().enabled();
    }

    /** 下单并返回二维码内容 code_url；渠道持续失败时熔断，直接提示稍后重试 */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "createNativeOrderFallback")
    public String createNativeOrder(Order order) {
        PaymentProperties.Wechat wechat = requireConfig();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appid", wechat.appId());
            body.put("mchid", wechat.mchId());
            body.put("description", "商城订单 " + order.getOrderNo());
            body.put("out_trade_no", order.getOrderNo());
            body.put("notify_url", properties.notifyBaseUrl() + "/api/payments/wechat/notify");
            body.put("amount", Map.of("total", toFen(order.getTotalAmount()), "currency", "CNY"));
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + NATIVE_PATH))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", buildAuthorization("POST", NATIVE_PATH, json))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("微信支付下单失败，HTTP {}", response.statusCode());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付下单失败，请稍后重试");
            }
            String codeUrl = objectMapper.readTree(response.body()).path("code_url").asText();
            if (!StringUtils.hasText(codeUrl)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付未返回二维码");
            }
            return codeUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微信支付下单异常: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付下单失败，请稍后重试");
        }
    }

    /** 熔断打开时快速降级，避免继续等待渠道超时 */
    private String createNativeOrderFallback(Order order, Throwable throwable) {
        log.warn("微信支付下单熔断降级：{}", throwable.getMessage());
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付渠道暂时不可用，请稍后重试");
    }

    /**
     * 回调验签并解密资源数据。验签失败、证书序列号不符或时间戳过期都会抛异常。
     */
    public WechatPaymentNotification verifyAndDecryptNotify(String body, String timestamp, String nonce,
                                                            String signature, String serial) {
        PaymentProperties.Wechat wechat = requireConfig();
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature) || !StringUtils.hasText(serial)) {
            throw new IllegalArgumentException("回调缺少签名头");
        }

        long callbackTime;
        try {
            callbackTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("回调时间戳非法");
        }
        if (Math.abs(Instant.now().getEpochSecond() - callbackTime) > MAX_CLOCK_SKEW_SECONDS) {
            throw new IllegalArgumentException("回调已过期");
        }

        String expectedSerial = platformCertificate().getSerialNumber().toString(16).toUpperCase(Locale.ROOT);
        if (!expectedSerial.equalsIgnoreCase(serial)) {
            throw new IllegalArgumentException("平台证书序列号不匹配");
        }
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        if (!verifySignature(message, signature)) {
            throw new IllegalArgumentException("回调签名校验失败");
        }

        try {
            JsonNode resource = objectMapper.readTree(body).path("resource");
            String ciphertext = resource.path("ciphertext").asText();
            if (!StringUtils.hasText(ciphertext)) {
                throw new IllegalArgumentException("回调数据为空");
            }
            String plain = decryptResource(
                    resource.path("associated_data").asText(""),
                    resource.path("nonce").asText(),
                    ciphertext,
                    wechat.apiV3Key());
            JsonNode node = objectMapper.readTree(plain);
            return new WechatPaymentNotification(
                    node.path("out_trade_no").asText(null),
                    node.path("transaction_id").asText(null),
                    node.path("trade_state").asText(null),
                    node.path("amount").path("total").asInt(-1),
                    node.path("appid").asText(null),
                    node.path("mchid").asText(null));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("回调数据解密失败");
        }
    }

    /** 校验回调里的商户信息，防止其他商户的通知被误用 */
    public boolean matchesMerchant(String appId, String mchId) {
        PaymentProperties.Wechat wechat = properties.wechat();
        return wechat != null
                && wechat.appId().equals(appId)
                && wechat.mchId().equals(mchId);
    }

    /**
     * 微信支付退款：调用 APIv3 退款接口，支持部分退款。
     * 渠道不可用时返回失败，由对账任务稍后重试，避免售后状态与资金状态不一致。
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "refundFallback")
    public RefundResult refund(Order order, BigDecimal amount, String refundNo, String reason) {
        PaymentProperties.Wechat wechat = requireConfig();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("out_trade_no", order.getOrderNo());
            body.put("out_refund_no", refundNo);
            if (StringUtils.hasText(reason)) {
                body.put("reason", reason.length() > 80 ? reason.substring(0, 80) : reason);
            }
            body.put("amount", Map.of(
                    "refund", toFen(amount),
                    "total", toFen(order.getTotalAmount()),
                    "currency", "CNY"));
            String json = objectMapper.writeValueAsString(body);
            HttpResponse<String> response = send("POST", REFUND_PATH, json);
            if (response.statusCode() != 200) {
                log.warn("微信退款失败，HTTP {}：{}", response.statusCode(), response.body());
                return RefundResult.fail("微信退款失败");
            }
            String status = objectMapper.readTree(response.body()).path("status").asText("");
            // SUCCESS 表示退款成功，PROCESSING 表示已受理，稍后到账
            if ("SUCCESS".equals(status) || "PROCESSING".equals(status)) {
                return RefundResult.ok(refundNo);
            }
            return RefundResult.fail("微信退款状态异常：" + status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微信退款异常：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信退款调用失败，请稍后重试");
        }
    }

    private RefundResult refundFallback(Order order, BigDecimal amount, String refundNo,
                                        String reason, Throwable throwable) {
        log.warn("微信退款熔断降级，订单 {}：{}", order.getOrderNo(), throwable.getMessage());
        return RefundResult.fail("微信支付渠道暂时不可用，请稍后重试");
    }

    /**
     * 查询交易状态，用于支付对账。订单不存在返回 NOTPAY，调用失败返回 null。
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "queryTradeStateFallback")
    public String queryTradeState(String orderNo) {
        PaymentProperties.Wechat wechat = requireConfig();
        try {
            String path = ORDER_QUERY_PREFIX + urlEncodePath(orderNo) + "?mchid=" + wechat.mchId();
            HttpResponse<String> response = send("GET", path, "");
            if (response.statusCode() == 404) {
                return "NOTPAY";
            }
            if (response.statusCode() != 200) {
                return null;
            }
            return objectMapper.readTree(response.body()).path("trade_state").asText(null);
        } catch (Exception e) {
            log.warn("微信支付单查询失败，订单 {}：{}", orderNo, e.getMessage());
            return null;
        }
    }

    /** 熔断打开时按查询失败处理，对账任务下一轮会重试 */
    private String queryTradeStateFallback(String orderNo, Throwable throwable) {
        log.warn("微信支付单查询熔断降级，订单 {}：{}", orderNo, throwable.getMessage());
        return null;
    }

    /** 发送 APIv3 请求，GET 时 path 需包含查询串参与签名 */
    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", buildAuthorization(method, path, json));
        if ("GET".equals(method)) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String urlEncodePath(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private PaymentProperties.Wechat requireConfig() {
        PaymentProperties.Wechat wechat = properties.wechat();
        if (wechat == null || !wechat.enabled()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付未开启");
        }
        if (!StringUtils.hasText(wechat.appId())
                || !StringUtils.hasText(wechat.mchId())
                || !StringUtils.hasText(wechat.serialNo())
                || !StringUtils.hasText(wechat.privateKeyPath())
                || !StringUtils.hasText(wechat.apiV3Key())
                || !StringUtils.hasText(wechat.platformCertPath())
                || !StringUtils.hasText(properties.notifyBaseUrl())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付参数未配置完整");
        }
        if (wechat.apiV3Key().length() != 32) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付 APIv3 密钥长度必须为 32");
        }
        return wechat;
    }

    /** 生成微信支付 APIv3 请求签名头 */
    private String buildAuthorization(String method, String path, String body) {
        PaymentProperties.Wechat wechat = properties.wechat();
        long timestamp = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = sign(message);
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + wechat.mchId()
                + "\",nonce_str=\"" + nonce
                + "\",signature=\"" + signature
                + "\",timestamp=\"" + timestamp
                + "\",serial_no=\"" + wechat.serialNo() + "\"";
    }

    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付签名失败");
        }
    }

    private boolean verifySignature(String message, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(platformCertificate().getPublicKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    private String decryptResource(String associatedData, String nonce, String ciphertext, String apiV3Key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("回调数据解密失败");
        }
    }

    private static int toFen(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private synchronized PrivateKey privateKey() {
        if (privateKey == null) {
            privateKey = PemKeyLoader.loadPrivateKey(properties.wechat().privateKeyPath());
        }
        return privateKey;
    }

    private synchronized X509Certificate platformCertificate() {
        if (platformCertificate == null) {
            platformCertificate = PemKeyLoader.loadCertificate(properties.wechat().platformCertPath());
        }
        return platformCertificate;
    }
}
