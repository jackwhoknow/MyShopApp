package com.example.myshopapp.order;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.order.dto.AfterSaleRequest;
import com.example.myshopapp.order.dto.AfterSaleVO;
import com.example.myshopapp.order.dto.ReturnShipmentRequest;
import com.example.myshopapp.order.entity.AfterSale;
import com.example.myshopapp.order.entity.AfterSaleStatus;
import com.example.myshopapp.order.entity.AfterSaleType;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.AfterSaleRepository;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.order.seckill.SeckillStockService;
import com.example.myshopapp.payment.PaymentService;
import com.example.myshopapp.payment.dto.RefundResult;
import com.example.myshopapp.points.PointService;
import com.example.myshopapp.product.repository.ProductSkuRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import com.example.myshopapp.upload.ImageUrlPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 售后：未发货走仅退款，已发货/已完成走退货退款。
 * 退货退款流程：申请 -> 商家同意退货 -> 买家寄回 -> 商家确认收货 -> 退款并回补库存。
 */
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AfterSaleRepository afterSaleRepository;
    private final SubOrderRepository subOrderRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final SeckillStockService seckillStockService;
    private final PointService pointService;
    private final ImageUrlPolicy imageUrlPolicy;

    @Transactional
    public AfterSaleVO apply(Long userId, AfterSaleRequest request) {
        SubOrder subOrder = subOrderRepository.findByIdAndOrderUserId(request.subOrderId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "子订单不存在"));

        OrderStatus orderStatus = subOrder.getOrder().getStatus();
        if (orderStatus != OrderStatus.PAID && orderStatus != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有已支付或已完成的订单可以申请售后");
        }
        AfterSaleType type = resolveType(subOrder.getStatus());
        if (afterSaleRepository.existsBySubOrderIdAndStatus(subOrder.getId(), AfterSaleStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该子订单已有处理中的售后申请");
        }

        AfterSale afterSale = new AfterSale();
        afterSale.setAfterSaleNo(generateNo("AS"));
        afterSale.setSubOrder(subOrder);
        afterSale.setUserId(userId);
        afterSale.setShopId(subOrder.getShop().getId());
        afterSale.setAmount(subOrder.getTotalAmount());
        afterSale.setReason(request.reason().trim());
        afterSale.setEvidenceImages(normalizeImages(request.evidenceImages()));
        afterSale.setType(type);
        afterSale.setStatus(AfterSaleStatus.PENDING);

        // 售后处理中：商家侧不可发货
        subOrder.setStatus(SubOrderStatus.REFUNDING);
        afterSaleRepository.save(afterSale);

        notificationService.notify(
                subOrder.getShop().getOwner().getId(),
                NotificationType.AFTER_SALE_APPLIED,
                "收到售后申请",
                "售后单 " + afterSale.getAfterSaleNo() + " 待处理："
                        + (type == AfterSaleType.REFUND_ONLY ? "仅退款" : "退货退款"),
                "/merchant/after-sales");
        return AfterSaleVO.from(afterSale);
    }

    @Transactional(readOnly = true)
    public PageResult<AfterSaleVO> listMine(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResult.of(afterSaleRepository.findByUserIdOrderByIdDesc(userId, pageable)
                .map(AfterSaleVO::from));
    }

    @Transactional(readOnly = true)
    public PageResult<AfterSaleVO> listForShop(Long ownerId, AfterSaleStatus status, int page, int size) {
        Shop shop = requireShop(ownerId);
        Pageable pageable = PageRequest.of(page, size);
        Page<AfterSale> result = status == null
                ? afterSaleRepository.findByShopIdOrderByIdDesc(shop.getId(), pageable)
                : afterSaleRepository.findByShopIdAndStatusOrderByIdDesc(shop.getId(), status, pageable);
        return PageResult.of(result.map(AfterSaleVO::from));
    }

    /** 平台售后监管：管理员跨店铺查看全部售后单 */
    @Transactional(readOnly = true)
    public PageResult<AfterSaleVO> listAll(AfterSaleStatus status, String keyword,
                                           LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = normalizedKeyword != null;
        return PageResult.of(afterSaleRepository.searchAdmin(
                        status != null, status,
                        hasKeyword, hasKeyword ? "%" + normalizedKeyword + "%" : null,
                        startDate != null, startDate != null ? startDate.atStartOfDay() : null,
                        endDate != null, endDate != null ? endDate.plusDays(1).atStartOfDay() : null,
                        pageable)
                .map(AfterSaleVO::from));
    }

    /** 平台介入退款：待处理或退货中的售后单由平台直接退款并回补库存 */
    @Transactional
    public AfterSaleVO platformRefund(Long afterSaleId, String remark) {
        AfterSale afterSale = requireIntervenable(afterSaleId, remark);
        SubOrder subOrder = afterSale.getSubOrder();

        afterSale.setAdminRemark(remark.trim());
        refundAndCancel(afterSale, subOrder, afterSale.getMerchantRemark());
        notifyPlatformIntervened(afterSale, subOrder, true);
        return AfterSaleVO.from(afterSale);
    }

    /** 平台介入关闭售后：不退款，子订单恢复到申请前状态 */
    @Transactional
    public AfterSaleVO platformClose(Long afterSaleId, String remark) {
        AfterSale afterSale = requireIntervenable(afterSaleId, remark);
        SubOrder subOrder = afterSale.getSubOrder();

        subOrder.setStatus(resolveOriginalStatus(subOrder));
        afterSale.setAdminRemark(remark.trim());
        afterSale.setStatus(AfterSaleStatus.CLOSED);
        if (afterSale.getHandledAt() == null) {
            afterSale.setHandledAt(LocalDateTime.now());
        }
        notifyPlatformIntervened(afterSale, subOrder, false);
        return AfterSaleVO.from(afterSale);
    }

    /**
     * 商家处理：
     * 1. 仅退款：直接退款、回补库存、子订单取消；
     * 2. 退货退款：同意退货并记录退货地址，等待买家寄回。
     */
    @Transactional
    public AfterSaleVO approve(Long ownerId, Long afterSaleId, String merchantRemark) {
        AfterSale afterSale = requireShopAfterSale(ownerId, afterSaleId);
        requirePending(afterSale);

        SubOrder subOrder = afterSale.getSubOrder();
        if (subOrder.getStatus() != SubOrderStatus.REFUNDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "子订单当前状态不可处理");
        }

        String remark = StringUtils.hasText(merchantRemark) ? merchantRemark.trim() : null;
        if (resolveExistingType(afterSale) == AfterSaleType.RETURN_REFUND) {
            if (remark == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "请填写退货地址或退货说明");
            }
            afterSale.setStatus(AfterSaleStatus.RETURNING);
            afterSale.setMerchantRemark(remark);
            afterSale.setHandledAt(LocalDateTime.now());
            notificationService.notify(
                    afterSale.getUserId(),
                    NotificationType.AFTER_SALE_APPROVED,
                    "商家已同意退货",
                    "售后单 " + afterSale.getAfterSaleNo() + " 商家已同意退货，请填写退货物流寄回商品",
                    "/orders/" + subOrder.getOrder().getId());
            return AfterSaleVO.from(afterSale);
        }

        refundAndCancel(afterSale, subOrder, remark);
        notifyRefunded(afterSale, subOrder);
        return AfterSaleVO.from(afterSale);
    }

    /**
     * 买家填写退货物流：只有商家同意退货、且尚未填写过物流时可用。
     */
    @Transactional
    public AfterSaleVO returnShipment(Long userId, Long afterSaleId, ReturnShipmentRequest request) {
        AfterSale afterSale = afterSaleRepository.findByIdAndUserId(afterSaleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "售后单不存在"));

        if (afterSale.getStatus() != AfterSaleStatus.RETURNING) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前售后单不在退货中，不能填写退货物流");
        }
        if (StringUtils.hasText(afterSale.getReturnTrackingNo())) {
            throw new BusinessException(ErrorCode.CONFLICT, "退货物流已填写，请等待商家确认收货");
        }

        afterSale.setReturnCompany(request.returnCompany().trim());
        afterSale.setReturnTrackingNo(request.returnTrackingNo().trim());
        afterSale.setReturnedAt(LocalDateTime.now());

        notificationService.notify(
                afterSale.getSubOrder().getShop().getOwner().getId(),
                NotificationType.AFTER_SALE_RETURN_SHIPPED,
                "买家已寄回退货",
                "售后单 " + afterSale.getAfterSaleNo() + " 买家已寄回，物流："
                        + afterSale.getReturnCompany() + "，运单号：" + afterSale.getReturnTrackingNo(),
                "/merchant/after-sales");
        return AfterSaleVO.from(afterSale);
    }

    /**
     * 驳回售后：子订单恢复到申请前的状态，买家可以再次申请。
     */
    @Transactional
    public AfterSaleVO reject(Long ownerId, Long afterSaleId, String merchantRemark) {
        AfterSale afterSale = requireShopAfterSale(ownerId, afterSaleId);
        requirePending(afterSale);

        SubOrder subOrder = afterSale.getSubOrder();
        if (subOrder.getStatus() != SubOrderStatus.REFUNDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "子订单当前状态不可驳回");
        }
        subOrder.setStatus(resolveOriginalStatus(subOrder));

        afterSale.setStatus(AfterSaleStatus.REJECTED);
        String remark = StringUtils.hasText(merchantRemark) ? merchantRemark.trim() : null;
        afterSale.setMerchantRemark(remark);
        afterSale.setHandledAt(LocalDateTime.now());

        notificationService.notify(
                afterSale.getUserId(),
                NotificationType.AFTER_SALE_REJECTED,
                "售后申请被驳回",
                "售后单 " + afterSale.getAfterSaleNo() + " 被商家驳回"
                        + (remark != null ? "：" + remark : ""),
                "/orders/" + subOrder.getOrder().getId());
        return AfterSaleVO.from(afterSale);
    }

    /**
     * 商家确认收到退货：退款、回补库存、子订单取消。
     */
    @Transactional
    public AfterSaleVO confirmReceipt(Long ownerId, Long afterSaleId) {
        AfterSale afterSale = requireShopAfterSale(ownerId, afterSaleId);
        if (afterSale.getStatus() != AfterSaleStatus.RETURNING) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有退货中的售后单可以确认收货");
        }

        SubOrder subOrder = afterSale.getSubOrder();
        if (subOrder.getStatus() != SubOrderStatus.REFUNDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "子订单当前状态不可退款");
        }

        refundAndCancel(afterSale, subOrder, afterSale.getMerchantRemark());
        afterSale.setReceivedAt(LocalDateTime.now());
        notifyRefunded(afterSale, subOrder);
        return AfterSaleVO.from(afterSale);
    }

    /**
     * 系统超时关闭：待处理售后超过时限仍未处理时自动关闭，子订单恢复原状态。
     */
    @Transactional
    public boolean autoCloseTimeout(Long afterSaleId, LocalDateTime cutoff) {
        AfterSale afterSale = afterSaleRepository.findById(afterSaleId).orElse(null);
        if (afterSale == null
                || afterSale.getStatus() != AfterSaleStatus.PENDING
                || afterSale.getCreatedAt() == null
                || !afterSale.getCreatedAt().isBefore(cutoff)) {
            return false;
        }

        SubOrder subOrder = afterSale.getSubOrder();
        if (subOrder.getStatus() == SubOrderStatus.REFUNDING) {
            subOrder.setStatus(resolveOriginalStatus(subOrder));
        }
        LocalDateTime now = LocalDateTime.now();
        afterSale.setStatus(AfterSaleStatus.CLOSED);
        afterSale.setClosedAt(now);
        afterSale.setHandledAt(now);
        afterSale.setMerchantRemark("商家超时未处理，系统自动关闭");

        String content = "售后单 " + afterSale.getAfterSaleNo() + " 超过处理时限，系统已自动关闭";
        notificationService.notify(afterSale.getUserId(), NotificationType.AFTER_SALE_AUTO_CLOSED,
                "售后已自动关闭", content + "，如需继续处理请重新申请", "/orders/" + subOrder.getOrder().getId());
        notificationService.notify(subOrder.getShop().getOwner().getId(),
                NotificationType.AFTER_SALE_AUTO_CLOSED, "售后超时已自动关闭", content, "/merchant/after-sales");
        return true;
    }

    /** 退款完成通知买家：金额取子订单实付金额 */
    private void notifyRefunded(AfterSale afterSale, SubOrder subOrder) {
        notificationService.notify(
                afterSale.getUserId(),
                NotificationType.AFTER_SALE_REFUNDED,
                "退款成功",
                "售后单 " + afterSale.getAfterSaleNo() + " 已退款 ¥"
                        + subOrder.getTotalAmount().toPlainString() + "，请注意查收",
                "/orders/" + subOrder.getOrder().getId());
    }

    /** 售后凭证只接受本站上传接口返回的图片地址 */
    private String normalizeImages(java.util.List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        java.util.List<String> valid = images.stream()
                .filter(imageUrlPolicy::allowed)
                .distinct()
                .toList();
        return valid.isEmpty() ? null : String.join(",", valid);
    }

    private AfterSaleType resolveType(SubOrderStatus status) {
        if (status == SubOrderStatus.WAIT_SHIP) {
            return AfterSaleType.REFUND_ONLY;
        }
        if (status == SubOrderStatus.SHIPPED || status == SubOrderStatus.COMPLETED) {
            return AfterSaleType.RETURN_REFUND;
        }
        throw new BusinessException(ErrorCode.CONFLICT, "当前子订单状态不可申请售后");
    }

    /** 历史数据没有类型字段时按仅退款处理 */
    private AfterSaleType resolveExistingType(AfterSale afterSale) {
        return afterSale.getType() == null ? AfterSaleType.REFUND_ONLY : afterSale.getType();
    }

    /** 驳回后恢复到申请前的子订单状态 */
    private SubOrderStatus resolveOriginalStatus(SubOrder subOrder) {
        if (subOrder.getCompletedAt() != null) {
            return SubOrderStatus.COMPLETED;
        }
        if (subOrder.getShippedAt() != null) {
            return SubOrderStatus.SHIPPED;
        }
        return SubOrderStatus.WAIT_SHIP;
    }

    /** 退款完成：先按原支付渠道退款，成功后再回补库存、取消子订单。 */
    private void refundAndCancel(AfterSale afterSale, SubOrder subOrder, String merchantRemark) {
        Order order = subOrder.getOrder();
        // 退款单号使用售后单号，重复调用时支付渠道可幂等处理，避免重复退款
        RefundResult refundResult = paymentService.refund(order, subOrder.getTotalAmount(),
                afterSale.getAfterSaleNo(), "售后退款：" + afterSale.getReason());
        if (!refundResult.success()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "退款未成功：" + refundResult.message() + "，请稍后重试");
        }

        for (OrderItem item : subOrder.getItems()) {
            productSkuRepository.findByIdForUpdate(item.getSkuId())
                    .ifPresent(sku -> sku.setStock(sku.getStock() + item.getQuantity()));
            // 退款成功回补库存后，同步 Redis 预扣库存
            seckillStockService.adjustAfterCommit(item.getSkuId(), item.getQuantity());
        }
        subOrder.setStatus(SubOrderStatus.CANCELLED);

        afterSale.setStatus(AfterSaleStatus.REFUNDED);
        afterSale.setMerchantRemark(merchantRemark);
        // 退货退款在同意退货时已记录处理时间，此时保留；仅退款直接以当前时间为准
        if (afterSale.getHandledAt() == null) {
            afterSale.setHandledAt(LocalDateTime.now());
        }

        boolean allCancelled = order.getSubOrders().stream()
                .allMatch(sub -> sub.getStatus() == SubOrderStatus.CANCELLED);
        if (allCancelled && order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            // 整单退完：退回抵扣积分并扣回已赠送积分，部分退款不动积分
            pointService.reverseOnFullRefund(order);
        }
    }

    private void requirePending(AfterSale afterSale) {
        if (afterSale.getStatus() != AfterSaleStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "该售后单已处理");
        }
    }

    /** 平台介入前提：填写介入说明，且售后单仍在待处理/退货中 */
    private AfterSale requireIntervenable(Long afterSaleId, String remark) {
        if (!StringUtils.hasText(remark)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写平台介入说明");
        }
        AfterSale afterSale = afterSaleRepository.findById(afterSaleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "售后单不存在"));
        if (afterSale.getStatus() != AfterSaleStatus.PENDING
                && afterSale.getStatus() != AfterSaleStatus.RETURNING) {
            throw new BusinessException(ErrorCode.CONFLICT, "该售后单已处理，不能再介入");
        }
        if (afterSale.getSubOrder().getStatus() != SubOrderStatus.REFUNDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "子订单当前状态不可介入");
        }
        return afterSale;
    }

    /** 平台介入结果同时通知买家与商家 */
    private void notifyPlatformIntervened(AfterSale afterSale, SubOrder subOrder, boolean refunded) {
        String result = refunded
                ? "已完成退款 ¥" + subOrder.getTotalAmount().toPlainString()
                : "已关闭售后，不退款";
        String content = "售后单 " + afterSale.getAfterSaleNo() + " 平台介入" + result
                + "，说明：" + afterSale.getAdminRemark();

        notificationService.notify(
                afterSale.getUserId(),
                NotificationType.AFTER_SALE_PLATFORM_INTERVENED,
                refunded ? "平台介入：退款完成" : "平台介入：售后关闭",
                content,
                "/orders/" + subOrder.getOrder().getId());
        notificationService.notify(
                subOrder.getShop().getOwner().getId(),
                NotificationType.AFTER_SALE_PLATFORM_INTERVENED,
                "平台介入处理售后",
                content,
                "/merchant/after-sales");
    }

    private AfterSale requireShopAfterSale(Long ownerId, Long afterSaleId) {
        Shop shop = requireShop(ownerId);
        return afterSaleRepository.findByIdAndShopId(afterSaleId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "售后单不存在"));
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法处理售后"));
    }

    private String generateNo(String prefix) {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefix + LocalDateTime.now().format(NO_FORMATTER) + random;
    }
}
