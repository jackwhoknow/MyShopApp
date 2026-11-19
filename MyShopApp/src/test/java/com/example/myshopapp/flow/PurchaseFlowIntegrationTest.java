package com.example.myshopapp.flow;

import com.example.myshopapp.auth.CaptchaService;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端交易链路回归：开店审核 -> 商品审核 -> 下单支付 -> 发货收货 -> 退货退款。
 * 使用 test profile 的独立库，测试事务结束后回滚。
 */
@SpringBootTest(properties = {
        "myshop.init.admin-username=admin",
        "myshop.init.admin-password=admin123",
        "myshop.payment.mock-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Transactional
class PurchaseFlowIntegrationTest {

    private static final String PASSWORD = "flow1238";

    @MockitoBean
    private CaptchaService captchaService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Test
    @DisplayName("完整交易链路：下单扣库存、支付、发货、收货、退货退款回补库存")
    void fullPurchaseAndRefundFlow() throws Exception {
        String adminToken = login("admin", "admin123");

        register("flow_merchant", true);
        register("flow_buyer", false);
        String merchantToken = login("flow_merchant", PASSWORD);
        String buyerToken = login("flow_buyer", PASSWORD);

        // 管理员建分类
        ApiResult category = postJson("/api/admin/categories", adminToken, Map.of(
                "name", "Flow Category", "sortOrder", 1, "enabled", true));
        assertThat(category.status).isEqualTo(200);
        long categoryId = category.body.path("data").path("id").asLong();

        // 商家开店 + 平台审核
        ApiResult shop = postJson("/api/merchant/shops/apply", merchantToken, Map.of(
                "name", "Flow Shop", "description", "integration test shop"));
        assertThat(shop.status).isEqualTo(200);
        assertThat(shop.body.path("data").path("status").asText()).isEqualTo("PENDING");
        long shopId = shop.body.path("data").path("id").asLong();

        ApiResult approveShop = postJson("/api/admin/shops/" + shopId + "/approve", adminToken, null);
        assertThat(approveShop.status).isEqualTo(200);
        assertThat(approveShop.body.path("data").path("status").asText()).isEqualTo("ACTIVE");

        // 商家建商品（两个 SKU）+ 平台审核
        Map<String, Object> skuRed = new LinkedHashMap<>();
        skuRed.put("price", new BigDecimal("100.00"));
        skuRed.put("stock", 10);
        skuRed.put("specValues", Map.of("Color", "Red"));
        Map<String, Object> skuBlue = new LinkedHashMap<>();
        skuBlue.put("price", new BigDecimal("120.00"));
        skuBlue.put("stock", 5);
        skuBlue.put("specValues", Map.of("Color", "Blue"));

        Map<String, Object> productBody = new LinkedHashMap<>();
        productBody.put("categoryId", categoryId);
        productBody.put("name", "Flow Product");
        productBody.put("subtitle", "integration test");
        productBody.put("description", "integration test product");
        productBody.put("mainImage", "https://example.com/p.png");
        productBody.put("specs", List.of(Map.of("name", "Color", "values", List.of("Red", "Blue"))));
        productBody.put("skus", List.of(skuRed, skuBlue));

        ApiResult product = postJson("/api/merchant/products", merchantToken, productBody);
        assertThat(product.status).isEqualTo(200);
        assertThat(product.body.path("data").path("status").asText()).isEqualTo("PENDING_REVIEW");
        long productId = product.body.path("data").path("id").asLong();
        long redSkuId = product.body.path("data").path("skus").get(0).path("id").asLong();

        assertThat(get("/api/products/" + productId, null).status).isEqualTo(404);

        ApiResult approveProduct = postJson("/api/admin/products/" + productId + "/approve", adminToken, null);
        assertThat(approveProduct.status).isEqualTo(200);

        // 买家地址 + 加购 + 下单
        ApiResult address = postJson("/api/addresses", buyerToken, Map.of(
                "receiverName", "Flow Buyer",
                "phone", "13800000003",
                "province", "Zhejiang",
                "city", "Hangzhou",
                "district", "Xihu",
                "detail", "No.1 Flow Road",
                "defaultAddress", true));
        assertThat(address.status).isEqualTo(200);
        long addressId = address.body.path("data").path("id").asLong();

        ApiResult cart = postJson("/api/cart", buyerToken, Map.of("skuId", redSkuId, "quantity", 2));
        assertThat(cart.status).isEqualTo(200);

        ApiResult order = postJson("/api/orders", buyerToken, Map.of(
                "addressId", addressId,
                "remark", "integration order",
                "cartItemIds", List.of(),
                "userCouponIds", List.of()));
        assertThat(order.status).isEqualTo(200);
        assertThat(order.body.path("data").path("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.body.path("data").path("totalAmount").decimalValue())
                .isEqualByComparingTo("200.00");
        long orderId = order.body.path("data").path("id").asLong();
        long subOrderId = order.body.path("data").path("subOrders").get(0).path("id").asLong();

        assertThat(skuStock(redSkuId)).isEqualTo(8);
        assertThat(get("/api/cart", buyerToken).body.path("data").size()).isZero();

        // 支付（重复支付被拒绝）
        ApiResult pay = postJson("/api/payments/" + orderId + "/prepare", buyerToken,
                Map.of("channel", "MOCK"));
        assertThat(pay.status).isEqualTo(200);
        assertThat(pay.body.path("data").path("paid").asBoolean()).isTrue();
        assertThat(postJson("/api/payments/" + orderId + "/prepare", buyerToken,
                Map.of("channel", "MOCK")).status).isEqualTo(409);

        // 商家发货 + 买家确认收货
        ApiResult merchantOrders = get("/api/merchant/orders?status=WAIT_SHIP&page=0&size=10", merchantToken);
        assertThat(merchantOrders.status).isEqualTo(200);
        assertThat(merchantOrders.body.path("data").path("list").size()).isGreaterThanOrEqualTo(1);

        ApiResult ship = postJson("/api/merchant/orders/" + subOrderId + "/ship", merchantToken,
                Map.of("shipCompany", "SF", "trackingNo", "SF0001"));
        assertThat(ship.status).isEqualTo(200);
        assertThat(ship.body.path("data").path("status").asText()).isEqualTo("SHIPPED");

        ApiResult confirm = postJson("/api/orders/" + orderId + "/confirm", buyerToken, null);
        assertThat(confirm.status).isEqualTo(200);
        assertThat(confirm.body.path("data").path("status").asText()).isEqualTo("COMPLETED");

        // 退货退款：商家同意 -> 买家寄回 -> 商家确认收货
        ApiResult afterSale = postJson("/api/after-sales", buyerToken, Map.of(
                "subOrderId", subOrderId, "reason", "integration return"));
        assertThat(afterSale.status).isEqualTo(200);
        assertThat(afterSale.body.path("data").path("type").asText()).isEqualTo("RETURN_REFUND");
        long afterSaleId = afterSale.body.path("data").path("id").asLong();

        ApiResult approveReturn = postJson("/api/merchant/after-sales/" + afterSaleId + "/approve",
                merchantToken, Map.of("merchantRemark", "send back to warehouse"));
        assertThat(approveReturn.status).isEqualTo(200);
        assertThat(approveReturn.body.path("data").path("status").asText()).isEqualTo("RETURNING");

        ApiResult returnShipment = postJson("/api/after-sales/" + afterSaleId + "/return-shipment",
                buyerToken, Map.of("returnCompany", "YTO", "returnTrackingNo", "YTO0001"));
        assertThat(returnShipment.status).isEqualTo(200);

        ApiResult confirmReceipt = postJson("/api/merchant/after-sales/" + afterSaleId + "/confirm-receipt",
                merchantToken, null);
        assertThat(confirmReceipt.status).isEqualTo(200);
        assertThat(confirmReceipt.body.path("data").path("status").asText()).isEqualTo("REFUNDED");

        assertThat(skuStock(redSkuId)).isEqualTo(10);
    }

    @Test
    @DisplayName("多店铺拆单：部分子订单退款取消后，剩余子订单仍可确认收货")
    void partialRefundOrderCanStillBeConfirmed() throws Exception {
        String adminToken = login("admin", "admin123");
        register("flow_merchant_a", true);
        register("flow_merchant_b", true);
        register("flow_multi_buyer", false);
        String tokenA = login("flow_merchant_a", PASSWORD);
        String tokenB = login("flow_merchant_b", PASSWORD);
        String buyerToken = login("flow_multi_buyer", PASSWORD);

        ApiResult category = postJson("/api/admin/categories", adminToken, Map.of(
                "name", "Multi Category", "sortOrder", 1, "enabled", true));
        long categoryId = category.body.path("data").path("id").asLong();

        long shopA = createShop(tokenA, adminToken, "Multi Shop A");
        long shopB = createShop(tokenB, adminToken, "Multi Shop B");
        long skuA = createProductAndGetSku(tokenA, adminToken, categoryId, "Multi Product A");
        long skuB = createProductAndGetSku(tokenB, adminToken, categoryId, "Multi Product B");

        ApiResult address = postJson("/api/addresses", buyerToken, Map.of(
                "receiverName", "Multi Buyer",
                "phone", "13800000004",
                "province", "Zhejiang",
                "city", "Hangzhou",
                "district", "Xihu",
                "detail", "No.2 Flow Road",
                "defaultAddress", true));
        long addressId = address.body.path("data").path("id").asLong();

        postJson("/api/cart", buyerToken, Map.of("skuId", skuA, "quantity", 1));
        postJson("/api/cart", buyerToken, Map.of("skuId", skuB, "quantity", 1));

        ApiResult order = postJson("/api/orders", buyerToken, Map.of(
                "addressId", addressId,
                "cartItemIds", List.of(),
                "userCouponIds", List.of()));
        assertThat(order.status).isEqualTo(200);
        assertThat(order.body.path("data").path("subOrders").size()).isEqualTo(2);
        long orderId = order.body.path("data").path("id").asLong();

        long subOrderA = 0;
        long subOrderB = 0;
        for (JsonNode sub : order.body.path("data").path("subOrders")) {
            if (sub.path("shopId").asLong() == shopA) {
                subOrderA = sub.path("id").asLong();
            } else if (sub.path("shopId").asLong() == shopB) {
                subOrderB = sub.path("id").asLong();
            }
        }
        assertThat(subOrderA).isPositive();
        assertThat(subOrderB).isPositive();

        assertThat(postJson("/api/payments/" + orderId + "/prepare", buyerToken,
                Map.of("channel", "MOCK")).status).isEqualTo(200);

        // A 店铺子订单申请仅退款并同意，子订单取消、主订单仍为已支付
        ApiResult afterSale = postJson("/api/after-sales", buyerToken, Map.of(
                "subOrderId", subOrderA, "reason", "multi shop refund"));
        long afterSaleId = afterSale.body.path("data").path("id").asLong();
        ApiResult approve = postJson("/api/merchant/after-sales/" + afterSaleId + "/approve", tokenA, null);
        assertThat(approve.status).isEqualTo(200);
        assertThat(approve.body.path("data").path("status").asText()).isEqualTo("REFUNDED");

        // B 店铺正常发货
        ApiResult ship = postJson("/api/merchant/orders/" + subOrderB + "/ship", tokenB,
                Map.of("shipCompany", "SF", "trackingNo", "SF-MULTI"));
        assertThat(ship.status).isEqualTo(200);

        // 已取消的子订单不阻塞整单确认收货
        ApiResult confirm = postJson("/api/orders/" + orderId + "/confirm", buyerToken, null);
        assertThat(confirm.status).isEqualTo(200);
        assertThat(confirm.body.path("data").path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("鉴权与参数校验：未登录 401、越权 403、畸形 JSON 400")
    void securityAndValidationBoundaries() throws Exception {
        register("flow_boundary_buyer", false);
        String buyerToken = login("flow_boundary_buyer", PASSWORD);

        assertThat(get("/api/orders", null).status).isEqualTo(401);
        assertThat(get("/api/admin/users", buyerToken).status).isEqualTo(403);
        assertThat(get("/api/merchant/orders", buyerToken).status).isEqualTo(403);

        MvcResult malformed = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"not-an-object\""))
                .andReturn();
        assertThat(malformed.getResponse().getStatus()).isEqualTo(400);

        assertThat(postJson("/api/cart", buyerToken, Map.of("skuId", 1, "quantity", 0)).status)
                .isEqualTo(400);
    }

    private long createShop(String merchantToken, String adminToken, String name) throws Exception {
        ApiResult shop = postJson("/api/merchant/shops/apply", merchantToken, Map.of(
                "name", name, "description", "multi shop"));
        assertThat(shop.status).isEqualTo(200);
        long shopId = shop.body.path("data").path("id").asLong();
        assertThat(postJson("/api/admin/shops/" + shopId + "/approve", adminToken, null).status)
                .isEqualTo(200);
        return shopId;
    }

    private long createProductAndGetSku(String merchantToken, String adminToken,
                                        long categoryId, String name) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("categoryId", categoryId);
        body.put("name", name);
        body.put("subtitle", "multi");
        body.put("description", "multi");
        body.put("mainImage", "https://example.com/m.png");
        body.put("specs", List.of(Map.of("name", "Color", "values", List.of("Red"))));
        body.put("skus", List.of(Map.of(
                "price", new BigDecimal("50.00"), "stock", 5, "specValues", Map.of("Color", "Red"))));

        ApiResult product = postJson("/api/merchant/products", merchantToken, body);
        assertThat(product.status).isEqualTo(200);
        long productId = product.body.path("data").path("id").asLong();
        assertThat(postJson("/api/admin/products/" + productId + "/approve", adminToken, null).status)
                .isEqualTo(200);
        return product.body.path("data").path("skus").get(0).path("id").asLong();
    }

    private void register(String username, boolean merchant) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", PASSWORD);
        body.put("nickname", username);
        body.put("phone", "");
        body.put("merchant", merchant);
        assertThat(postJson("/api/auth/register", null, body).status).isEqualTo(200);
    }

    private String login(String username, String password) throws Exception {
        ApiResult result = postJson("/api/auth/login", null,
                Map.of("username", username, "password", password,
                        "captchaId", "test-captcha", "captchaCode", "0"));
        assertThat(result.status).isEqualTo(200);
        return result.body.path("data").path("token").asText();
    }

    private int skuStock(long skuId) {
        return productSkuRepository.findById(skuId).orElseThrow().getStock();
    }

    private ApiResult get(String path, String token) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(path);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return exchange(builder);
    }

    private ApiResult postJson(String path, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            builder.content(objectMapper.writeValueAsString(body));
        }
        return exchange(builder);
    }

    private ApiResult exchange(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = content == null || content.isBlank() ? null : objectMapper.readTree(content);
        return new ApiResult(result.getResponse().getStatus(), json);
    }

    private record ApiResult(int status, JsonNode body) {
    }
}
