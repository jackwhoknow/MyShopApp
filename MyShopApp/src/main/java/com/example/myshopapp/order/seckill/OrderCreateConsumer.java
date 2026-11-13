package com.example.myshopapp.order.seckill;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.order.OrderService;
import com.example.myshopapp.order.dto.OrderCreateMessage;
import com.example.myshopapp.order.dto.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 下单队列消费者：真正落库创建订单。
 * <p>
 * 幂等由三层保证：排队状态已是终态直接跳过、订单表 request_id 唯一键、落库前校验购物车数量未变化。
 * 业务失败（库存不足、优惠券失效等）不回队列，直接置为失败并回补预扣库存；
 * 数据库等临时异常重试有限次数，仍失败则回补库存并提示买家重新提交。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "myshop.order.async.enabled", havingValue = "true", matchIfMissing = true)
public class OrderCreateConsumer {

    private final OrderService orderService;
    private final SeckillStockService stockService;
    private final AsyncOrderStatusService statusService;

    @Value("${myshop.order.async.max-attempts:3}")
    private int maxAttempts;

    @RabbitListener(queues = OrderMqConfig.CREATE_QUEUE)
    public void handle(OrderCreateMessage message) {
        AsyncOrderState state = statusService.find(message.requestId());
        if (state == null) {
            // 排队状态已过期（超过保留时长），无法回写结果，直接丢弃避免产生无主订单
            log.warn("下单请求状态已过期，忽略消息：requestId={}", message.requestId());
            return;
        }
        if (!"PENDING".equals(state.status())) {
            log.info("下单请求已处理，忽略重复消息：requestId={}，status={}",
                    message.requestId(), state.status());
            return;
        }

        Map<Long, Integer> current;
        try {
            current = orderService.resolveQuantities(message.userId(), message.cartItemIds());
        } catch (BusinessException e) {
            fail(message, e.getMessage());
            return;
        }
        if (!current.equals(message.quantities())) {
            fail(message, "购物车已发生变化，请重新提交订单");
            return;
        }

        for (int attempt = 1; ; attempt++) {
            try {
                OrderVO order = orderService.createWithRequestId(
                        message.userId(), message.toRequest(), message.requestId());
                statusService.markSuccess(message.requestId(), order.id(), order.totalAmount());
                log.info("异步下单完成：requestId={}，orderId={}", message.requestId(), order.id());
                return;
            } catch (BusinessException e) {
                fail(message, e.getMessage());
                return;
            } catch (RuntimeException e) {
                log.warn("异步下单异常（第 {} 次）：requestId={}", attempt, message.requestId(), e);
                if (attempt >= maxAttempts) {
                    fail(message, "下单失败，请稍后重试");
                    return;
                }
                sleepQuietly(200L * attempt);
            }
        }
    }

    /** 失败收尾：回补 Redis 预扣库存并写入失败原因，消息不再重投 */
    private void fail(OrderCreateMessage message, String reason) {
        stockService.restore(message.quantities());
        statusService.markFailed(message.requestId(), reason);
        log.info("异步下单失败：requestId={}，原因={}", message.requestId(), reason);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
