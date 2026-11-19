package com.example.myshopapp.logistics;

import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.SubOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 快递100 实时查询：按快递公司名称映射到快递100编码，调用实时查询接口。
 * 未配置账号、单号缺失或接口失败时返回空轨迹，绝不伪造物流数据。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "myshop.logistics", name = "provider", havingValue = "kuaidi100")
public class Kuaidi100LogisticsProvider implements LogisticsProvider {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** 常见快递公司名称到快递100编码的映射 */
    private static final Map<String, String> COMPANY_CODES = Map.ofEntries(
            Map.entry("顺丰", "shunfeng"),
            Map.entry("顺丰速运", "shunfeng"),
            Map.entry("顺丰快递", "shunfeng"),
            Map.entry("中通", "zhongtong"),
            Map.entry("中通快递", "zhongtong"),
            Map.entry("圆通", "yuantong"),
            Map.entry("圆通速递", "yuantong"),
            Map.entry("韵达", "yunda"),
            Map.entry("韵达快递", "yunda"),
            Map.entry("申通", "shentong"),
            Map.entry("申通快递", "shentong"),
            Map.entry("邮政", "ems"),
            Map.entry("中国邮政", "ems"),
            Map.entry("EMS", "ems"),
            Map.entry("京东", "jd"),
            Map.entry("京东物流", "jd"),
            Map.entry("极兔", "jtexpress"),
            Map.entry("极兔速递", "jtexpress"),
            Map.entry("德邦", "debangwuliu"),
            Map.entry("德邦快递", "debangwuliu"),
            Map.entry("百世", "huitongkuaidi"),
            Map.entry("百世快递", "huitongkuaidi"),
            Map.entry("天天", "tiantian"),
            Map.entry("天天快递", "tiantian"),
            Map.entry("宅急送", "zhaijisong"));

    private final LogisticsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public Kuaidi100LogisticsProvider(LogisticsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "kuaidi100";
    }

    @Override
    public SubOrderLogisticsVO query(SubOrder subOrder) {
        String companyCode = resolveCompanyCode(subOrder.getShipCompany());
        List<SubOrderLogisticsVO.TraceVO> traces = List.of();
        if (companyCode == null) {
            log.warn("物流公司「{}」无法映射到快递100编码，子订单 {} 轨迹为空",
                    subOrder.getShipCompany(), subOrder.getSubOrderNo());
        } else if (StringUtils.hasText(subOrder.getTrackingNo())) {
            traces = fetchTraces(companyCode, subOrder.getTrackingNo());
        }
        return new SubOrderLogisticsVO(
                subOrder.getId(),
                subOrder.getSubOrderNo(),
                subOrder.getStatus().name(),
                subOrder.getShipCompany(),
                subOrder.getTrackingNo(),
                subOrder.getShippedAt(),
                subOrder.getCompletedAt(),
                traces);
    }

    private List<SubOrderLogisticsVO.TraceVO> fetchTraces(String companyCode, String trackingNo) {
        LogisticsProperties.Kuaidi100 config = properties.kuaidi100();
        String body = requestBody(companyCode, trackingNo, config);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.endpoint()))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText("");
            if (!"200".equals(status)) {
                log.warn("快递100查询失败，com={} num={}：{}", companyCode, trackingNo,
                        root.path("message").asText("未知错误"));
                return List.of();
            }
            List<SubOrderLogisticsVO.TraceVO> traces = new ArrayList<>();
            for (JsonNode node : root.path("data")) {
                traces.add(new SubOrderLogisticsVO.TraceVO(
                        node.path("status").asText("物流更新"),
                        node.path("context").asText(""),
                        node.path("areaName").asText(""),
                        parseTime(node.path("ftime").asText(node.path("time").asText(null)))));
            }
            return traces;
        } catch (Exception e) {
            log.warn("快递100查询异常，com={} num={}：{}", companyCode, trackingNo, e.getMessage());
            return List.of();
        }
    }

    /** 快递100 签名规则：MD5(param + key + customer).toUpperCase */
    private static String requestBody(String companyCode, String trackingNo,
                                      LogisticsProperties.Kuaidi100 config) {
        String param = "{\"com\":\"" + companyCode + "\",\"num\":\"" + trackingNo + "\"}";
        String sign = md5(param + config.key() + config.customer()).toUpperCase(Locale.ROOT);
        return "customer=" + encode(config.customer())
                + "&sign=" + encode(sign)
                + "&param=" + encode(param);
    }

    /** 公司名称映射为快递100编码；已经是编码形式时直接使用 */
    private static String resolveCompanyCode(String company) {
        if (!StringUtils.hasText(company)) {
            return null;
        }
        String trimmed = company.trim();
        String mapped = COMPANY_CODES.get(trimmed);
        if (mapped != null) {
            return mapped;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        String mappedUpper = COMPANY_CODES.get(upper);
        if (mappedUpper != null) {
            return mappedUpper;
        }
        return trimmed.matches("[a-z0-9]+") ? trimmed : null;
    }

    private static LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private static String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
