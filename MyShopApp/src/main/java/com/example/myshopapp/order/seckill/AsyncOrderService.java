package com.example.myshopapp.order.seckill;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.OrderService;
import com.example.myshopapp.order.dto.AsyncOrderVO;
import com.example.myshopapp.order.dto.OrderCreateMessage;
import com.example.myshopapp.order.dto.OrderVO;
import com.example.myshopapp.order.dto.SeckillOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 秒杀下单入口：Redis 原子预扣库存 -> 投递 MQ -> 立即返回排队编号，由消费者异步落库。
 * <p>
 * Redis 不可用时退回同步下单，数据库悲观锁仍能保证不超卖；
 * 关闭异步开关（本地/测试）时同样走同步下单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncOrderService {

    private final OrderService orderService;
    private final SeckillStockService stockService;
    private final AsyncOrderStatusService statusService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${myshop.order.async.enabled:true}")
    private boolean enabled;

    public AsyncOrderVO submit(Long userId, SeckillOrderRequest request) {
        // 同步模式：直接落库并返回结果，前端轮询第一次就能拿到终态
        if (!enabled) {
            OrderVO order = orderService.create(userId, request.toCreateRequest());
            return AsyncOrderVO.success(UUID.randomUUID().toString().replace("-", ""),
                    order.id(), order.totalAmount());
        }

        String requestId = StringUtils.hasText(request.requestId())
                ? request.requestId().trim()
                : UUID.randomUUID().toString().replace("-", "");
        if (!statusService.markPending(userId, requestId)) {
            // 同一个 requestId 重复提交（如网络重试）：直接返回已有排队状态
            return statusService.view(requestId, userId);
        }

        OrderService.CartSnapshot snapshot;
        try {
            snapshot = orderService.resolveCart(userId, request.cartItemIds());
        } catch (BusinessException e) {
            statusService.markFailed(requestId, e.getMessage());
            throw e;
        }

        SeckillStockService.DeductResult deducted;
        try {
            deducted = stockService.tryDeduct(snapshot.quantities());
        } catch (DataAccessException e) {
            log.warn("Redis 预扣库存不可用，退回同步下单：{}", e.getMessage());
            return createSynchronously(userId, request, requestId);
        }
        if (!deducted.success()) {
            statusService.markFailed(requestId, deducted.message());
            throw new BusinessException(ErrorCode.CONFLICT, deducted.message());
        }

        try {
            rabbitTemplate.convertAndSend(OrderMqConfig.CREATE_EXCHANGE, OrderMqConfig.CREATE_ROUTING_KEY,
                    new OrderCreateMessage(requestId, userId, request.addressId(), request.remark(),
                            snapshot.cartItemIds(), request.userCouponIds(), snapshot.quantities()));
        } catch (RuntimeException e) {
            // 投递失败必须把预扣的库存还回去，否则库存会被长期占用
            stockService.restore(snapshot.quantities());
            statusService.markFailed(requestId, "下单服务繁忙，请稍后重试");
            log.error("下单消息投递失败：requestId={}", requestId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下单服务繁忙，请稍后重试");
        }

        return AsyncOrderVO.pending(requestId);
    }

    /** 查询排队结果：请求不存在或不属于当前用户时按“不存在”处理 */
    public AsyncOrderVO status(Long userId, String requestId) {
        AsyncOrderVO view = statusService.view(requestId, userId);
        if (view == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "下单请求不存在或已过期");
        }
        return view;
    }

    private AsyncOrderVO createSynchronously(Long userId, SeckillOrderRequest request, String requestId) {
        try {
            OrderVO order = orderService.createSynchronously(userId, request.toCreateRequest(), requestId);
            statusService.markSuccess(requestId, order.id(), order.totalAmount());
            return AsyncOrderVO.success(requestId, order.id(), order.totalAmount());
        } catch (BusinessException e) {
            statusService.markFailed(requestId, e.getMessage());
            throw e;
        }
    }
}
