package com.example.myshopapp.payment.alipay;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.payment.config.PaymentProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import com.example.myshopapp.payment.dto.RefundResult;
import com.example.myshopapp.payment.support.PemKeyLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 支付宝电脑网站支付：服务端用应用私钥签名生成跳转链接，下单和支付密码都由支付宝页面完成，
 * 本站不接触任何支付密码、银行卡信息。
 */
@Slf4j
@Service
public class AlipayService {

    private static final String SIGN_TYPE = "RSA2";
    private static final String CHARSET = "utf-8";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

    private PrivateKey privateKey;
    private PublicKey alipayPublicKey;

    public AlipayService(PaymentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return properties.alipay() != null && properties.alipay().enabled();
    }

    /**
     * 生成电脑网站支付跳转地址（GET 方式），前端只需整页跳转或新窗口打开。
     */
    public String createPagePayUrl(Order order) {
        PaymentProperties.Alipay alipay = requireConfig();

        Map<String, String> params = new TreeMap<>();
        params.put("app_id", alipay.appId());
        params.put("method", "alipay.trade.page.pay");
        params.put("format", "JSON");
        params.put("charset", CHARSET);
        params.put("sign_type", SIGN_TYPE);
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        params.put("version", "1.0");
        params.put("notify_url", properties.notifyBaseUrl() + "/api/payments/alipay/notify");
        if (StringUtils.hasText(alipay.returnUrl())) {
            params.put("return_url", alipay.returnUrl());
        }

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount",
                order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", "商城订单 " + order.getOrderNo());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        try {
            params.put("biz_content", objectMapper.writeValueAsString(bizContent));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝下单参数生成失败");
        }

        // 待签名串包含 sign_type，与支付宝 SDK 的请求签名规则一致
        params.put("sign", sign(contentForSign(params, false)));

        String query = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return alipay.serverUrl() + "?" + query;
    }

    /**
     * 校验支付宝异步通知：RSA2 验签 + app_id / seller_id 校验。
     * 验签失败直接抛异常，调用方不得入账。
     */
    public void verifyNotify(Map<String, String> params) {
        PaymentProperties.Alipay alipay = requireConfig();
        String sign = params.get("sign");
        if (!StringUtils.hasText(sign)) {
            throw new IllegalArgumentException("回调缺少签名");
        }
        if (!SIGN_TYPE.equalsIgnoreCase(params.get("sign_type"))) {
            throw new IllegalArgumentException("回调签名类型不支持");
        }
        if (!alipay.appId().equals(params.get("app_id"))) {
            throw new IllegalArgumentException("回调 app_id 不匹配");
        }
        if (StringUtils.hasText(alipay.sellerId())
                && !alipay.sellerId().equals(params.get("seller_id"))) {
            throw new IllegalArgumentException("回调 seller_id 不匹配");
        }

        // 支付宝异步通知约定排除 sign、sign_type，这里兼容两种待签名串以适配不同接入版本
        boolean verified = verifySignature(contentForSign(params, true), sign)
                || verifySignature(contentForSign(params, false), sign);
        if (!verified) {
            throw new IllegalArgumentException("回调签名校验失败");
        }
    }

    /**
     * 原路退款：调用 alipay.trade.refund，退款金额不能超过订单实付金额。
     * 响应报文做 RSA2 验签，防止被篡改后误判为退款成功。
     * 渠道不可用时返回失败，由对账任务稍后重试。
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "refundFallback")
    public RefundResult refund(String orderNo, BigDecimal amount, String refundNo, String reason) {
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("refund_amount", amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("out_request_no", refundNo);
        if (StringUtils.hasText(reason)) {
            bizContent.put("refund_reason", reason.length() > 100 ? reason.substring(0, 100) : reason);
        }
        JsonNode response = call("alipay.trade.refund", bizContent);
        if ("10000".equals(response.path("code").asText())) {
            return RefundResult.ok(refundNo);
        }
        String message = response.path("sub_msg").asText(response.path("msg").asText("支付宝退款失败"));
        log.warn("支付宝退款失败，订单 {}：{}", orderNo, message);
        return RefundResult.fail(message);
    }

    private RefundResult refundFallback(String orderNo, BigDecimal amount, String refundNo,
                                        String reason, Throwable throwable) {
        log.warn("支付宝退款熔断降级，订单 {}：{}", orderNo, throwable.getMessage());
        return RefundResult.fail("支付宝渠道暂时不可用，请稍后重试");
    }

    /**
     * 查询交易状态，用于支付对账。返回支付宝的 trade_status，查询失败返回 null。
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "queryTradeStateFallback")
    public String queryTradeState(String orderNo) {
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", orderNo);
        JsonNode response = call("alipay.trade.query", bizContent);
        if (!"10000".equals(response.path("code").asText())) {
            return null;
        }
        return response.path("trade_status").asText(null);
    }

    /** 熔断打开时按查询失败处理，对账任务下一轮会重试 */
    private String queryTradeStateFallback(String orderNo, Throwable throwable) {
        log.warn("支付宝交易查询熔断降级，订单 {}：{}", orderNo, throwable.getMessage());
        return null;
    }

    /**
     * 发起支付宝开放平台接口调用，并校验响应签名。
     */
    private JsonNode call(String method, Map<String, Object> bizContent) {
        PaymentProperties.Alipay alipay = requireConfig();
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", alipay.appId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", CHARSET);
        params.put("sign_type", SIGN_TYPE);
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        params.put("version", "1.0");
        try {
            params.put("biz_content", objectMapper.writeValueAsString(bizContent));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝请求参数生成失败");
        }
        params.put("sign", sign(contentForSign(params, false)));

        String form = params.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(alipay.serverUrl()))
                    .timeout(HTTP_TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "支付宝接口响应异常（HTTP " + response.statusCode() + "）");
            }
            String nodeName = method.replace('.', '_') + "_response";
            verifyResponseSign(response.body(), nodeName);
            return objectMapper.readTree(response.body()).path(nodeName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用支付宝接口 {} 失败：{}", method, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝接口调用失败，请稍后重试");
        }
    }

    /** 校验开放平台响应签名：签名串为响应业务节点的原始 JSON 文本 */
    private void verifyResponseSign(String body, String nodeName) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String sign = root.path("sign").asText(null);
            if (!StringUtils.hasText(sign)) {
                throw new IllegalArgumentException("响应缺少签名");
            }
            String content = extractNodeJson(body, nodeName);
            if (content == null || !verifySignature(content, sign)) {
                throw new IllegalArgumentException("响应签名校验失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝响应签名校验失败");
        }
    }

    /** 从原始 JSON 中截取指定节点的完整 JSON 文本，用于验签 */
    private String extractNodeJson(String body, String nodeName) {
        String key = "\"" + nodeName + "\"";
        int keyIndex = body.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }
        int start = body.indexOf('{', keyIndex + key.length());
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return body.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private PaymentProperties.Alipay requireConfig() {
        PaymentProperties.Alipay alipay = properties.alipay();
        if (alipay == null || !alipay.enabled()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝支付未开启");
        }
        if (!StringUtils.hasText(alipay.appId())
                || !StringUtils.hasText(alipay.privateKeyPath())
                || !StringUtils.hasText(alipay.alipayPublicKeyPath())
                || !StringUtils.hasText(alipay.serverUrl())
                || !StringUtils.hasText(properties.notifyBaseUrl())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝支付参数未配置完整");
        }
        return alipay;
    }

    /** 构造待签名串：按参数名升序，跳过空值；excludeSignType 用于异步通知验签 */
    private String contentForSign(Map<String, String> params, boolean excludeSignType) {
        return params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !(excludeSignType && "sign_type".equals(entry.getKey())))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String sign(String content) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝签名失败");
        }
    }

    private boolean verifySignature(String content, String sign) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(alipayPublicKey());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized PrivateKey privateKey() {
        if (privateKey == null) {
            privateKey = PemKeyLoader.loadPrivateKey(properties.alipay().privateKeyPath());
        }
        return privateKey;
    }

    private synchronized PublicKey alipayPublicKey() {
        if (alipayPublicKey == null) {
            alipayPublicKey = PemKeyLoader.loadPublicKey(properties.alipay().alipayPublicKeyPath());
        }
        return alipayPublicKey;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
