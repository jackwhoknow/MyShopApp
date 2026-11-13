package com.example.myshopapp.payment;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.order.OrderService;
import com.example.myshopapp.payment.alipay.AlipayService;
import com.example.myshopapp.payment.dto.PaymentPrepareRequest;
import com.example.myshopapp.payment.dto.PaymentPrepareVO;
import com.example.myshopapp.payment.dto.PaymentStatusVO;
import com.example.myshopapp.payment.wechat.WechatPayService;
import com.example.myshopapp.payment.wechat.WechatPaymentNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付接口：买家发起支付、轮询结果，以及两个支付平台的服务器回调。
 * 买家只在支付宝/微信官方页面输入支付密码，本站不收集任何支付凭证。
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AlipayService alipayService;
    private final WechatPayService wechatPayService;
    private final OrderService orderService;

    @PostMapping("/{orderId}/prepare")
    public ApiResponse<PaymentPrepareVO> prepare(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long orderId,
                                                 @RequestBody(required = false) PaymentPrepareRequest request) {
        return ApiResponse.ok(paymentService.prepare(loginUser.id(), orderId,
                request == null ? null : request.channel()));
    }

    @GetMapping("/{orderId}/status")
    public ApiResponse<PaymentStatusVO> status(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.status(loginUser.id(), orderId));
    }

    /**
     * 支付宝异步通知：不做登录鉴权，但必须先通过 RSA2 验签，再校验金额与订单状态。
     * 支付宝服务端默认用 POST 表单通知，同时兼容 GET；应答 success 后不再重试。
     */
    @RequestMapping(value = "/alipay/notify", method = {RequestMethod.GET, RequestMethod.POST},
            produces = "text/plain;charset=UTF-8")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        try {
            alipayService.verifyNotify(params);
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return "success";
            }
            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");
            if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(tradeNo)
                    || !StringUtils.hasText(totalAmount)) {
                return "failure";
            }
            boolean accepted = orderService.settlePayment(orderNo, new BigDecimal(totalAmount),
                    PaymentChannel.ALIPAY.name(), tradeNo).accepted();
            return accepted ? "success" : "failure";
        } catch (Exception e) {
            // 不记录回调原始参数，避免泄露交易信息
            log.warn("支付宝回调处理失败: {}", e.getMessage());
            return "failure";
        }
    }

    /**
     * 微信支付回调：平台证书验签 + APIv3 密钥解密 + 商户号校验，通过后才允许入账。
     */
    @PostMapping("/wechat/notify")
    public ResponseEntity<Map<String, String>> wechatNotify(
            @RequestBody String body,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial) {
        try {
            WechatPaymentNotification notification =
                    wechatPayService.verifyAndDecryptNotify(body, timestamp, nonce, signature, serial);
            if (!wechatPayService.matchesMerchant(notification.appId(), notification.mchId())) {
                throw new IllegalArgumentException("商户信息不匹配");
            }
            if (!"SUCCESS".equals(notification.tradeState())) {
                return wechatSuccess();
            }
            if (!StringUtils.hasText(notification.outTradeNo())
                    || !StringUtils.hasText(notification.transactionId())
                    || notification.totalFen() < 0) {
                throw new IllegalArgumentException("回调数据不完整");
            }
            boolean accepted = orderService.settlePayment(notification.outTradeNo(),
                    notification.totalAmount(), PaymentChannel.WECHAT.name(),
                    notification.transactionId()).accepted();
            return accepted ? wechatSuccess() : wechatFail();
        } catch (Exception e) {
            log.warn("微信支付回调处理失败: {}", e.getMessage());
            return wechatFail();
        }
    }

    private ResponseEntity<Map<String, String>> wechatSuccess() {
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
    }

    private ResponseEntity<Map<String, String>> wechatFail() {
        return ResponseEntity.status(400).body(Map.of("code", "FAIL", "message", "处理失败"));
    }
}
