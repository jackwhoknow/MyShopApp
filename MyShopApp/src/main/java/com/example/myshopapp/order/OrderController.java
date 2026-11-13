package com.example.myshopapp.order;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.order.dto.AsyncOrderVO;
import com.example.myshopapp.order.dto.CreateOrderRequest;
import com.example.myshopapp.order.dto.OrderVO;
import com.example.myshopapp.order.dto.ReorderResultVO;
import com.example.myshopapp.order.dto.SeckillOrderRequest;
import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.seckill.AsyncOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 买家订单接口：创建、列表、详情、取消、确认收货。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderReorderService orderReorderService;
    private final AsyncOrderService asyncOrderService;

    /**
     * 秒杀下单：Redis 预扣库存后进入排队队列，立即返回 requestId，前端轮询结果。
     * 库存不足等前置校验失败会直接返回错误，不再占用队列。
     */
    @PostMapping("/seckill")
    public ApiResponse<AsyncOrderVO> seckill(@AuthenticationPrincipal LoginUser loginUser,
                                             @Valid @RequestBody SeckillOrderRequest request) {
        return ApiResponse.ok(asyncOrderService.submit(loginUser.id(), request));
    }

    /** 查询排队下单结果：PENDING / SUCCESS / FAILED */
    @GetMapping("/seckill/{requestId}")
    public ApiResponse<AsyncOrderVO> seckillStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable String requestId) {
        return ApiResponse.ok(asyncOrderService.status(loginUser.id(), requestId));
    }

    /** 一键复购：把该订单商品按当前库存加入购物车，不可购买的商品跳过 */
    @PostMapping("/{id}/reorder")
    public ApiResponse<ReorderResultVO> reorder(@AuthenticationPrincipal LoginUser loginUser,
                                                @PathVariable Long id) {
        return ApiResponse.ok(orderReorderService.reorder(loginUser.id(), id));
    }

    @PostMapping
    public ApiResponse<OrderVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(loginUser.id(), request));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderVO>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.list(loginUser.id(), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderVO> detail(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(loginUser.id(), id));
    }

    /** 子订单物流轨迹（模拟数据），仅订单所属买家可查看 */
    @GetMapping("/{id}/sub-orders/{subOrderId}/logistics")
    public ApiResponse<SubOrderLogisticsVO> logistics(@AuthenticationPrincipal LoginUser loginUser,
                                                      @PathVariable Long id,
                                                      @PathVariable Long subOrderId) {
        return ApiResponse.ok(orderService.logistics(loginUser.id(), id, subOrderId));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderVO> cancel(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id) {
        return ApiResponse.ok(orderService.cancel(loginUser.id(), id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderVO> confirm(@AuthenticationPrincipal LoginUser loginUser,
                                        @PathVariable Long id) {
        return ApiResponse.ok(orderService.confirm(loginUser.id(), id));
    }
}
