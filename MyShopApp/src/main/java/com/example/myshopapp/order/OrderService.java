package com.example.myshopapp.order;

import com.example.myshopapp.address.entity.UserAddress;
import com.example.myshopapp.address.repository.UserAddressRepository;
import com.example.myshopapp.cart.entity.CartItem;
import com.example.myshopapp.cart.repository.CartItemRepository;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.logistics.LogisticsService;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.coupon.entity.Coupon;
import com.example.myshopapp.coupon.entity.UserCoupon;
import com.example.myshopapp.coupon.entity.UserCouponStatus;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import com.example.myshopapp.order.dto.CreateOrderRequest;
import com.example.myshopapp.order.dto.OrderVO;
import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.PaymentRecord;
import com.example.myshopapp.order.entity.PaymentStatus;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.OrderItemRepository;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.order.repository.PaymentRecordRepository;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.order.seckill.SeckillStockService;
import com.example.myshopapp.points.PointService;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.entity.ProductSku;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import com.example.myshopapp.product.support.SkuDescriber;
import com.example.myshopapp.promotion.PromotionService;
import com.example.myshopapp.promotion.entity.ShopPromotion;
import com.example.myshopapp.seckill.SeckillActivityService;
import com.example.myshopapp.seckill.entity.SeckillActivity;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单业务：结算拆单、库存扣减、支付入账、取消回补库存。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    /** 进程内序列，避免同一毫秒的订单号碰撞 */
    private static final AtomicLong NO_SEQUENCE = new AtomicLong();

    /** 积分最多抵扣扣券后应付金额的一半 */
    private static final BigDecimal POINTS_MAX_RATIO = new BigDecimal("0.5");

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final NotificationService notificationService;
    private final SeckillStockService seckillStockService;
    private final SeckillActivityService seckillActivityService;
    private final PointService pointService;
    private final LogisticsService logisticsService;
    private final PromotionService promotionService;

    /**
     * 从购物车创建订单：
     * 1. 收货信息快照；
     * 2. 按店铺拆子订单；
     * 3. 悲观锁扣减 SKU 库存；
     * 4. 删除已下单的购物车项。
     */
    @Transactional
    public OrderVO create(Long userId, CreateOrderRequest request) {
        return doCreate(userId, request, null, true);
    }

    /**
     * 同步下单兜底（Redis 不可用或未开启异步）：订单落库后同步扣减 Redis 预扣库存，
     * 保证秒杀库存键与数据库一致。
     */
    @Transactional
    public OrderVO createSynchronously(Long userId, CreateOrderRequest request, String requestId) {
        return doCreate(userId, request, requestId, true);
    }

    /**
     * 带排队编号的创建订单：异步消费可能重复投递同一条消息，
     * requestId 已生成订单时直接返回原订单，避免重复下单与重复扣库存。
     * 此路径的 SKU 库存已在提交阶段由 Redis 预扣，落库时不再改动 Redis。
     */
    @Transactional
    public OrderVO createWithRequestId(Long userId, CreateOrderRequest request, String requestId) {
        return doCreate(userId, request, requestId, false);
    }

    private OrderVO doCreate(Long userId, CreateOrderRequest request, String requestId,
                             boolean deductRedisStock) {
        if (StringUtils.hasText(requestId)) {
            Optional<Order> existing = orderRepository.findByRequestId(requestId);
            if (existing.isPresent()) {
                return OrderVO.from(existing.get());
            }
        }

        UserAddress address = userAddressRepository.findByIdAndUserId(request.addressId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在"));

        List<CartItem> cartItems = loadCartItems(userId, request.cartItemIds());
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先选择要结算的商品");
        }

        // 店铺 id -> 该店铺参与结算的购物车项。LinkedHashMap 保证拆单顺序稳定
        Map<Long, List<CartItem>> itemsByShop = new LinkedHashMap<>();
        // 商品 id -> 本次下单数量，用于限购校验
        Map<Long, Integer> productQuantities = new LinkedHashMap<>();
        // 当前进行中的秒杀活动：skuId -> 活动，下单时按活动价结算并校验限购
        Map<Long, SeckillActivity> seckillBySku = seckillActivityService.runningBySku();
        for (CartItem cartItem : cartItems) {
            ProductSku sku = productSkuRepository.findByIdForUpdate(cartItem.getSku().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品已不存在"));
            Product product = sku.getProduct();

            if (product.getStatus() != ProductStatus.ON_SHELF) {
                throw new BusinessException(ErrorCode.CONFLICT, "商品「" + product.getName() + "」已下架");
            }
            if (product.getShop().getStatus() != ShopStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "店铺「" + product.getShop().getName() + "」已暂停营业");
            }
            if (sku.getStock() < cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "商品「" + product.getName() + "」库存不足，仅剩 " + sku.getStock() + " 件");
            }

            // 限购校验：历史已购 + 本次下单不能超过商品限购数量
            int orderedNow = productQuantities.merge(product.getId(),
                    cartItem.getQuantity(), Integer::sum);
            if (product.getPurchaseLimit() > 0) {
                long bought = orderItemRepository.sumPurchasedQuantity(product.getId(), userId);
                if (bought + orderedNow > product.getPurchaseLimit()) {
                    throw new BusinessException(ErrorCode.CONFLICT,
                            "商品「" + product.getName() + "」每人限购 " + product.getPurchaseLimit()
                                    + " 件，你已购买 " + bought + " 件");
                }
            }

            // 事务内扣减，后续任何一步失败都会整体回滚
            sku.setStock(sku.getStock() - cartItem.getQuantity());
            itemsByShop.computeIfAbsent(product.getShop().getId(), key -> new ArrayList<>()).add(cartItem);
        }

        Order order = new Order();
        order.setOrderNo(generateNo("OD"));
        order.setRequestId(requestId);
        order.setUser(userRepository.getReferenceById(userId));
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.fullAddress());
        order.setRemark(request.remark());

        // 校验并锁定本次使用的券：店铺券每店最多一张，平台券可与店铺券叠加
        CouponSelection coupons =
                resolveCoupons(userId, request.userCouponIds(), itemsByShop.keySet());

        List<UserCoupon> usedCoupons = new ArrayList<>();
        List<SubOrderDraft> drafts = new ArrayList<>();
        int shopIndex = 1;
        for (Map.Entry<Long, List<CartItem>> entry : itemsByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<CartItem> shopCartItems = entry.getValue();

            SubOrder subOrder = new SubOrder();
            subOrder.setOrder(order);
            subOrder.setShop(shopCartItems.get(0).getSku().getProduct().getShop());
            subOrder.setSubOrderNo(order.getOrderNo() + "-" + shopIndex++);
            subOrder.setStatus(SubOrderStatus.WAIT_SHIP);

            BigDecimal subTotal = BigDecimal.ZERO;
            for (CartItem cartItem : shopCartItems) {
                ProductSku sku = cartItem.getSku();
                Product product = sku.getProduct();

                // 命中正在进行的秒杀活动时，先加锁校验活动库存与每人限购，再按活动价下单
                SeckillActivity seckill = seckillBySku.get(sku.getId());
                BigDecimal unitPrice = sku.getPrice();
                if (seckill != null) {
                    SeckillActivity consumed = seckillActivityService.consume(
                            userId, seckill.getId(), cartItem.getQuantity());
                    unitPrice = consumed.getActivityPrice();
                }

                OrderItem item = new OrderItem();
                item.setSubOrder(subOrder);
                item.setProductId(product.getId());
                item.setSkuId(sku.getId());
                item.setSeckillActivityId(seckill == null ? null : seckill.getId());
                item.setProductName(product.getName());
                item.setSkuDesc(SkuDescriber.describe(sku));
                item.setImage(StringUtils.hasText(sku.getImage()) ? sku.getImage() : product.getMainImage());
                item.setPrice(unitPrice);
                item.setQuantity(cartItem.getQuantity());
                item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                subOrder.getItems().add(item);

                subTotal = subTotal.add(item.getTotalPrice());
            }

            // 满减活动：按店铺小计匹配优惠力度最大的活动，先于优惠券计算
            BigDecimal promotionDiscount = BigDecimal.ZERO;
            ShopPromotion promotion = promotionService.bestFor(shopId, subTotal).orElse(null);
            if (promotion != null) {
                promotionDiscount = promotion.getReduceAmount().min(subTotal);
                subOrder.setPromotionDiscount(promotionDiscount);
                subOrder.setPromotionTitle(promotion.getTitle());
            }

            UserCoupon userCoupon = coupons.shopCoupons().get(shopId);
            if (userCoupon != null) {
                Coupon coupon = userCoupon.getCoupon();
                if (subTotal.compareTo(coupon.getThresholdAmount()) < 0) {
                    throw new BusinessException(ErrorCode.CONFLICT,
                            "优惠券「" + coupon.getTitle() + "」需满 "
                                    + coupon.getThresholdAmount().stripTrailingZeros().toPlainString() + " 元可用");
                }
                subOrder.setDiscountAmount(coupon.getDiscountAmount());
                subOrder.setCouponTitle(coupon.getTitle());
                usedCoupons.add(userCoupon);
            }
            drafts.add(new SubOrderDraft(subOrder, subTotal));
        }

        // 平台券只作用于一个子订单：自动选满足门槛且金额最高的那一单
        if (coupons.platformCoupon() != null) {
            UserCoupon platformCoupon = coupons.platformCoupon();
            Coupon coupon = platformCoupon.getCoupon();
            SubOrderDraft target = drafts.stream()
                    .filter(draft -> draft.subTotal().compareTo(coupon.getThresholdAmount()) >= 0)
                    .max(Comparator.comparing(SubOrderDraft::subTotal))
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                            "优惠券「" + coupon.getTitle() + "」需满 "
                                    + coupon.getThresholdAmount().stripTrailingZeros().toPlainString() + " 元可用"));
            SubOrder subOrder = target.subOrder();
            BigDecimal shopDiscount = subOrder.getDiscountAmount() == null
                    ? BigDecimal.ZERO : subOrder.getDiscountAmount();
            BigDecimal remaining = target.subTotal()
                    .subtract(subOrder.getPromotionDiscount() == null
                            ? BigDecimal.ZERO : subOrder.getPromotionDiscount())
                    .subtract(shopDiscount)
                    .max(BigDecimal.ZERO);
            BigDecimal platformDiscount = coupon.getDiscountAmount().min(remaining);
            subOrder.setDiscountAmount(shopDiscount.add(platformDiscount));
            subOrder.setCouponTitle(subOrder.getCouponTitle() == null
                    ? coupon.getTitle() : subOrder.getCouponTitle() + " + " + coupon.getTitle());
            usedCoupons.add(platformCoupon);
        }

        BigDecimal orderTotal = BigDecimal.ZERO;
        for (SubOrderDraft draft : drafts) {
            SubOrder subOrder = draft.subOrder();
            BigDecimal discount = subOrder.getDiscountAmount() == null
                    ? BigDecimal.ZERO : subOrder.getDiscountAmount();
            BigDecimal promotionOff = subOrder.getPromotionDiscount() == null
                    ? BigDecimal.ZERO : subOrder.getPromotionDiscount();
            BigDecimal payable = draft.subTotal().subtract(promotionOff).subtract(discount);
            subOrder.setTotalAmount(payable);
            orderTotal = orderTotal.add(payable);
            order.getSubOrders().add(subOrder);
        }

        // 积分抵扣：按扣券后应付金额的 50% 封顶，且必须为 100 积分的整数倍
        int points = request.points() == null ? 0 : request.points();
        if (points < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "积分数量不正确");
        }
        if (points > 0) {
            if (points % 100 != 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "积分必须为 100 的整数倍");
            }
            BigDecimal pointsDiscount = BigDecimal.valueOf(points).movePointLeft(2);
            if (pointsDiscount.compareTo(orderTotal.multiply(POINTS_MAX_RATIO)) > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "积分抵扣不能超过应付金额的 50%");
            }
            // 抵扣金额按各子订单应付金额比例分摊，最后一单吸收分位尾差
            List<SubOrder> subOrders = order.getSubOrders();
            BigDecimal allocated = BigDecimal.ZERO;
            for (int i = 0; i < subOrders.size(); i++) {
                SubOrder subOrder = subOrders.get(i);
                BigDecimal share = i == subOrders.size() - 1
                        ? pointsDiscount.subtract(allocated)
                        : pointsDiscount.multiply(subOrder.getTotalAmount())
                                .divide(orderTotal, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(share);
                subOrder.setTotalAmount(subOrder.getTotalAmount().subtract(share));
            }
            order.setPointsUsed(points);
            order.setPointsDiscount(pointsDiscount);
            orderTotal = orderTotal.subtract(pointsDiscount);
        }
        order.setTotalAmount(orderTotal);

        orderRepository.save(order);

        // 订单落库拿到 id 后再扣积分，流水可关联订单；余额不足时整个事务回滚
        if (points > 0) {
            pointService.deduct(userId, points, order.getId(), order.getOrderNo());
        }

        // 券的核销要等订单落库拿到 id 后再回填
        if (!usedCoupons.isEmpty()) {
            LocalDateTime usedAt = LocalDateTime.now();
            usedCoupons.forEach(userCoupon -> {
                userCoupon.setStatus(UserCouponStatus.USED);
                userCoupon.setOrderId(order.getId());
                userCoupon.setUsedAt(usedAt);
            });
        }

        if (deductRedisStock) {
            for (CartItem cartItem : cartItems) {
                seckillStockService.adjustAfterCommit(cartItem.getSku().getId(), -cartItem.getQuantity());
            }
        }

        cartItemRepository.deleteAll(cartItems);
        return OrderVO.from(order);
    }

    /**
     * 解析本次结算的购物车项与 SKU 数量，供秒杀预扣库存使用。
     * cartItemIds 为空表示结算整个购物车。
     */
    @Transactional(readOnly = true)
    public CartSnapshot resolveCart(Long userId, List<Long> cartItemIds) {
        List<CartItem> cartItems = loadCartItems(userId, cartItemIds);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先选择要结算的商品");
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CartItem cartItem : cartItems) {
            quantities.merge(cartItem.getSku().getId(), cartItem.getQuantity(), Integer::sum);
        }
        return new CartSnapshot(cartItems.stream().map(CartItem::getId).toList(), quantities);
    }

    /** 只取 SKU 数量：消费端落库前校验购物车是否与提交时一致 */
    @Transactional(readOnly = true)
    public Map<Long, Integer> resolveQuantities(Long userId, List<Long> cartItemIds) {
        return resolveCart(userId, cartItemIds).quantities();
    }

    private List<CartItem> loadCartItems(Long userId, List<Long> cartItemIds) {
        return cartItemIds == null || cartItemIds.isEmpty()
                ? cartItemRepository.findByUserIdOrderByIdDesc(userId)
                : cartItemRepository.findByUserIdAndIdIn(userId, cartItemIds);
    }

    /** 一次结算涉及的购物车项与 SKU 数量快照 */
    public record CartSnapshot(List<Long> cartItemIds, Map<Long, Integer> quantities) {
    }

    private record CouponSelection(Map<Long, UserCoupon> shopCoupons, UserCoupon platformCoupon) {
    }

    private record SubOrderDraft(SubOrder subOrder, BigDecimal subTotal) {
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> list(Long userId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = status == null
                ? orderRepository.findByUserIdOrderByIdDesc(userId, pageable)
                : orderRepository.findByUserIdAndStatusOrderByIdDesc(userId, status, pageable);
        return PageResult.of(orders.map(OrderVO::from));
    }

    @Transactional(readOnly = true)
    public OrderVO detail(Long userId, Long orderId) {
        return OrderVO.from(requireOwnedOrder(userId, orderId));
    }

    /**
     * 买家查看子订单物流轨迹：子订单必须属于当前用户且挂在指定主订单下。
     */
    @Transactional(readOnly = true)
    public SubOrderLogisticsVO logistics(Long userId, Long orderId, Long subOrderId) {
        SubOrder subOrder = subOrderRepository.findByIdAndOrderUserId(subOrderId, userId)
                .filter(item -> item.getOrder().getId().equals(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "子订单不存在"));
        return logisticsService.query(subOrder);
    }

    /**
     * 支付入账：支付宝/微信回调与模拟支付统一走这里。
     * 以第三方交易号做幂等，金额必须与订单一致；订单已关闭时不改状态，只记录待退款并提醒买家。
     */
    @Transactional
    public SettlementResult settlePayment(String orderNo, BigDecimal amount, String channel, String tradeNo) {
        Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (order == null) {
            return SettlementResult.fail("订单不存在");
        }
        // 同一笔第三方交易只入账一次，支付平台重试回调时直接按成功应答
        String payNo = normalizePayNo(tradeNo);
        if (paymentRecordRepository.existsByPayNo(payNo)) {
            return SettlementResult.ok("重复回调，已处理");
        }

        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPayNo(payNo);
        record.setAmount(amount);
        record.setChannel(channel);

        if (amount == null || amount.compareTo(order.getTotalAmount()) != 0) {
            record.setStatus(PaymentStatus.FAILED);
            paymentRecordRepository.save(record);
            return SettlementResult.fail("支付金额与订单金额不一致");
        }

        LocalDateTime now = LocalDateTime.now();
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            // 钱已到账但订单已关闭（例如超时取消），记为待退款并提醒买家联系客服
            record.setStatus(PaymentStatus.REFUND_REQUIRED);
            paymentRecordRepository.save(record);
            notificationService.notify(order.getUser().getId(), NotificationType.ORDER_PAID,
                    "订单已关闭，支付款待退回",
                    "订单 " + orderNo + " 在支付完成前已关闭，款项将由平台退回，请联系客服处理",
                    "/orders/" + order.getId());
            return SettlementResult.ok("订单已关闭，已记录待退款");
        }

        record.setStatus(PaymentStatus.SUCCESS);
        record.setPaidAt(now);
        paymentRecordRepository.save(record);

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);

        // 支付成功按实付金额返还积分：1 元 1 积分，不足 1 元向下取整
        int earned = order.getTotalAmount().intValue();
        if (earned > 0) {
            order.setPointsEarned(earned);
            pointService.earn(order.getUser().getId(), earned, order.getId(), orderNo);
        }

        // 支付成功：通知买家本人，并通知每个出单店铺的店主
        notificationService.notify(order.getUser().getId(), NotificationType.ORDER_PAID, "支付成功",
                "订单 " + orderNo + " 已支付成功，商家正在备货", "/orders/" + order.getId());
        notifyOwners(order.getSubOrders(), NotificationType.NEW_ORDER, "收到新订单",
                "订单 " + orderNo + " 买家已付款，请及时发货");
        return SettlementResult.ok("支付成功");
    }

    /** 支付入账结果：accepted=true 表示回调已被正常处理，支付平台无需重试 */
    public record SettlementResult(boolean accepted, String message) {

        public static SettlementResult ok(String message) {
            return new SettlementResult(true, message);
        }

        public static SettlementResult fail(String message) {
            return new SettlementResult(false, message);
        }
    }

    /**
     * 取消订单：仅限待支付，取消时把已扣的库存按子订单商品回补。
     */
    @Transactional
    public OrderVO cancel(Long userId, Long orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有待支付订单可以取消");
        }
        doCancel(order, false);
        return OrderVO.from(order);
    }

    /**
     * 系统超时取消：待支付且创建时间早于截止时间才处理，返回是否取消成功。
     * 定时任务逐单调用，每单独立事务，避免个别异常影响其余订单。
     */
    @Transactional
    public boolean cancelTimeoutOrder(Long orderId, LocalDateTime cutoff) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null
                || order.getStatus() != OrderStatus.PENDING_PAYMENT
                || order.getCreatedAt() == null
                || !order.getCreatedAt().isBefore(cutoff)) {
            return false;
        }
        doCancel(order, true);
        return true;
    }

    /** 取消订单公共逻辑：子订单置为已取消、回补库存、退回优惠券并通知买卖双方 */
    private void doCancel(Order order, boolean timeout) {
        for (SubOrder subOrder : order.getSubOrders()) {
            subOrder.setStatus(SubOrderStatus.CANCELLED);
            for (OrderItem item : subOrder.getItems()) {
                productSkuRepository.findByIdForUpdate(item.getSkuId()).ifPresent(sku ->
                        sku.setStock(sku.getStock() + item.getQuantity()));
                // 数据库提交后再同步 Redis 预扣库存，避免事务回滚造成 Redis 多补
                seckillStockService.adjustAfterCommit(item.getSkuId(), item.getQuantity());
                if (item.getSeckillActivityId() != null) {
                    seckillActivityService.restore(item.getSeckillActivityId(), item.getQuantity());
                }
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        // 取消后把用掉的券退回，买家可继续用于其他订单
        releaseCoupons(order.getId());
        // 取消后把下单抵扣的积分退回
        if (order.getPointsUsed() > 0) {
            pointService.returnOnCancel(order.getUser().getId(), order.getPointsUsed(),
                    order.getId(), order.getOrderNo());
        }

        if (timeout) {
            notificationService.notify(order.getUser().getId(), NotificationType.ORDER_CANCELLED,
                    "订单已自动取消",
                    "订单 " + order.getOrderNo() + " 超过支付时限未支付，系统已自动取消，库存与优惠券已退回",
                    "/orders/" + order.getId());
        }
        notifyOwners(order.getSubOrders(), NotificationType.ORDER_CANCELLED,
                timeout ? "订单已超时取消" : "订单已取消",
                "订单 " + order.getOrderNo() + (timeout
                        ? " 买家超时未支付，系统已自动取消"
                        : " 已被买家取消"));
    }

    /**
     * 买家确认收货：剩余未取消的子订单都已发货时，整单完成。
     * 已退款取消的子订单不参与收货判断，否则部分退款的拆单订单会永远无法完成。
     */
    @Transactional
    public OrderVO confirm(Long userId, Long orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有已支付订单可以确认收货");
        }
        List<SubOrder> activeSubOrders = order.getSubOrders().stream()
                .filter(sub -> sub.getStatus() != SubOrderStatus.CANCELLED)
                .toList();
        boolean allShipped = !activeSubOrders.isEmpty()
                && activeSubOrders.stream()
                        .allMatch(sub -> sub.getStatus() == SubOrderStatus.SHIPPED);
        if (!allShipped) {
            throw new BusinessException(ErrorCode.CONFLICT, "还有子订单未发货，暂不能确认收货");
        }

        LocalDateTime now = LocalDateTime.now();
        activeSubOrders.forEach(sub -> {
            sub.setStatus(SubOrderStatus.COMPLETED);
            sub.setCompletedAt(now);
        });
        order.setStatus(OrderStatus.COMPLETED);
        notifyOwners(activeSubOrders, NotificationType.ORDER_COMPLETED, "买家已确认收货",
                "订单 " + order.getOrderNo() + " 买家已确认收货，交易完成");
        return OrderVO.from(order);
    }

    /**
     * 系统自动确认收货：已发货且超过自动确认时限的子订单单独完成，
     * 主订单下全部有效子订单都完成后主订单置为已完成。
     */
    @Transactional
    public boolean autoCompleteSubOrder(Long subOrderId, LocalDateTime cutoff) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId).orElse(null);
        if (subOrder == null
                || subOrder.getStatus() != SubOrderStatus.SHIPPED
                || subOrder.getShippedAt() == null
                || !subOrder.getShippedAt().isBefore(cutoff)) {
            return false;
        }

        subOrder.setStatus(SubOrderStatus.COMPLETED);
        subOrder.setCompletedAt(LocalDateTime.now());

        Order order = subOrder.getOrder();
        boolean allCompleted = order.getSubOrders().stream()
                .filter(sub -> sub.getStatus() != SubOrderStatus.CANCELLED)
                .allMatch(sub -> sub.getStatus() == SubOrderStatus.COMPLETED);
        if (allCompleted && order.getStatus() != OrderStatus.COMPLETED) {
            order.setStatus(OrderStatus.COMPLETED);
        }

        notificationService.notify(
                order.getUser().getId(),
                NotificationType.ORDER_AUTO_COMPLETED,
                "订单已自动确认收货",
                "子订单 " + subOrder.getSubOrderNo() + " 发货已超过约定时间，系统已自动确认收货",
                "/orders/" + order.getId());
        notifyOwners(List.of(subOrder), NotificationType.ORDER_COMPLETED, "订单已自动完成",
                "子订单 " + subOrder.getSubOrderNo() + " 超时未确认收货，系统已自动完成");
        return true;
    }

    /** 同一订单的多家店铺各发一条通知，同一店主只通知一次 */
    private void notifyOwners(List<SubOrder> subOrders, NotificationType type, String title, String content) {
        Set<Long> notifiedOwnerIds = new HashSet<>();
        for (SubOrder subOrder : subOrders) {
            Long ownerId = subOrder.getShop().getOwner().getId();
            if (notifiedOwnerIds.add(ownerId)) {
                notificationService.notify(ownerId, type, title, content, "/merchant/orders");
            }
        }
    }

    private Order requireOwnedOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
    }

    /**
     * 校验本次使用的券：归属当前用户、未使用、未过期、店铺在本次订单中、同店只能一张。
     * 门槛校验在拆单计算小计时进行，因为需要知道该店铺的实际商品金额。
     */
    /**
     * 解析本次结算使用的券：店铺券按店铺归组（每店最多一张），
     * 平台券（shop 为空）单独返回且每笔订单最多一张。
     */
    private CouponSelection resolveCoupons(Long userId, List<Long> userCouponIds, Set<Long> orderShopIds) {
        if (userCouponIds == null || userCouponIds.isEmpty()) {
            return new CouponSelection(Map.of(), null);
        }
        List<Long> distinctIds = userCouponIds.stream().distinct().toList();
        List<UserCoupon> userCoupons = userCouponRepository.findByIdInAndUserId(distinctIds, userId);
        if (userCoupons.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<Long, UserCoupon> shopCoupons = new LinkedHashMap<>();
        UserCoupon platformCoupon = null;
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = userCoupon.getCoupon();
            if (userCoupon.getStatus() != UserCouponStatus.UNUSED) {
                throw new BusinessException(ErrorCode.CONFLICT, "优惠券「" + coupon.getTitle() + "」已使用");
            }
            if (!now.isBefore(coupon.getEndAt())) {
                throw new BusinessException(ErrorCode.CONFLICT, "优惠券「" + coupon.getTitle() + "」已过期");
            }
            if (coupon.getShop() == null) {
                if (platformCoupon != null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "每笔订单只能使用一张平台券");
                }
                platformCoupon = userCoupon;
                continue;
            }
            Long shopId = coupon.getShop().getId();
            if (!orderShopIds.contains(shopId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "优惠券「" + coupon.getTitle() + "」不适用于当前订单");
            }
            if (shopCoupons.putIfAbsent(shopId, userCoupon) != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "同一店铺只能使用一张优惠券");
            }
        }
        return new CouponSelection(shopCoupons, platformCoupon);
    }

    /** 订单取消：把该订单用掉的券恢复为未使用 */
    private void releaseCoupons(Long orderId) {
        userCouponRepository.findByOrderIdAndStatus(orderId, UserCouponStatus.USED).forEach(userCoupon -> {
            userCoupon.setStatus(UserCouponStatus.UNUSED);
            userCoupon.setOrderId(null);
            userCoupon.setUsedAt(null);
        });
    }

    /** 订单号 = 前缀 + 毫秒时间 + 4 位序列 + 3 位随机数，长度 26 字符，低于字段上限 32 */
    private String generateNo(String prefix) {
        long sequence = NO_SEQUENCE.getAndIncrement() % 10000;
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return prefix + LocalDateTime.now().format(NO_FORMATTER)
                + String.format("%04d%03d", sequence, random);
    }

    /** 支付流水号超过字段上限时压成哈希，保证幂等键稳定且不截断 */
    private String normalizePayNo(String tradeNo) {
        String value = tradeNo == null ? "" : tradeNo;
        return value.length() <= 32 ? value : "H" + Integer.toHexString(value.hashCode());
    }
}
