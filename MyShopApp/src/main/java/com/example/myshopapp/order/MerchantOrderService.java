package com.example.myshopapp.order;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.order.dto.BatchShipRequest;
import com.example.myshopapp.order.dto.BatchShipResultVO;
import com.example.myshopapp.order.dto.MerchantRemarkRequest;
import com.example.myshopapp.order.dto.MerchantSubOrderVO;
import com.example.myshopapp.order.dto.ShipRequest;
import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商家履约：查看自己店铺的子订单并执行发货。
 */
@Service
@RequiredArgsConstructor
public class MerchantOrderService {

    /** 只有已支付或已完成的主订单才进入履约视野，待支付订单不展示 */
    private static final List<OrderStatus> FULFILLABLE_ORDER_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.COMPLETED);

    private final SubOrderRepository subOrderRepository;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResult<MerchantSubOrderVO> list(Long ownerId, SubOrderStatus status, String keyword,
                                               LocalDate startDate, LocalDate endDate, int page, int size) {
        Shop shop = requireShop(ownerId);
        Pageable pageable = PageRequest.of(page, size);

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = normalizedKeyword != null;
        LocalDateTime startAt = startDate != null ? startDate.atStartOfDay() : null;
        // 结束日期按“当天 23:59:59”处理，即次日 0 点作为开区间上界
        LocalDateTime endAt = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        Page<SubOrder> subOrders = subOrderRepository.searchByShop(
                shop.getId(),
                FULFILLABLE_ORDER_STATUSES,
                status != null,
                status != null ? status : SubOrderStatus.WAIT_SHIP,
                hasKeyword,
                hasKeyword ? "%" + normalizedKeyword + "%" : null,
                startAt != null,
                startAt,
                endAt != null,
                endAt,
                pageable);

        return PageResult.of(subOrders.map(MerchantSubOrderVO::from));
    }

    @Transactional(readOnly = true)
    public MerchantSubOrderVO detail(Long ownerId, Long subOrderId) {
        Shop shop = requireShop(ownerId);
        return MerchantSubOrderVO.from(requireOwnedSubOrder(shop.getId(), subOrderId));
    }

    /** 商家查看自己店铺子订单的物流轨迹 */
    @Transactional(readOnly = true)
    public SubOrderLogisticsVO logistics(Long ownerId, Long subOrderId) {
        Shop shop = requireShop(ownerId);
        return SubOrderLogisticsVO.from(requireOwnedSubOrder(shop.getId(), subOrderId));
    }

    /**
     * 发货：仅已支付主订单下的待发货子订单可操作，写入物流信息并置为 SHIPPED。
     */
    @Transactional
    public MerchantSubOrderVO ship(Long ownerId, Long subOrderId, ShipRequest request) {
        Shop shop = requireShop(ownerId);
        SubOrder subOrder = requireOwnedSubOrder(shop.getId(), subOrderId);
        validateShippable(subOrder);

        doShip(subOrder, request.shipCompany(), request.trackingNo());
        return MerchantSubOrderVO.from(subOrder);
    }

    /**
     * 修改物流：仅已发货的子订单可改，保留未填写的原有字段。
     */
    @Transactional
    public MerchantSubOrderVO updateLogistics(Long ownerId, Long subOrderId, ShipRequest request) {
        Shop shop = requireShop(ownerId);
        SubOrder subOrder = requireOwnedSubOrder(shop.getId(), subOrderId);
        if (subOrder.getStatus() != SubOrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有已发货的订单可以修改物流信息");
        }
        if (!StringUtils.hasText(request.shipCompany()) && !StringUtils.hasText(request.trackingNo())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请至少填写物流公司或运单号");
        }
        if (StringUtils.hasText(request.shipCompany())) {
            subOrder.setShipCompany(request.shipCompany().trim());
        }
        if (StringUtils.hasText(request.trackingNo())) {
            subOrder.setTrackingNo(request.trackingNo().trim());
        }
        return MerchantSubOrderVO.from(subOrder);
    }

    /**
     * 更新商家内部备注：任何已归属的子订单都可写，空白内容表示清空。
     */
    @Transactional
    public MerchantSubOrderVO updateRemark(Long ownerId, Long subOrderId, MerchantRemarkRequest request) {
        Shop shop = requireShop(ownerId);
        SubOrder subOrder = requireOwnedSubOrder(shop.getId(), subOrderId);
        String remark = request.remark();
        subOrder.setMerchantRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        return MerchantSubOrderVO.from(subOrder);
    }

    /**
     * 批量发货：逐单校验，失败的订单记录原因但不影响其余订单。
     */
    @Transactional
    public BatchShipResultVO batchShip(Long ownerId, BatchShipRequest request) {
        Shop shop = requireShop(ownerId);
        List<Long> subOrderIds = request.subOrderIds().stream().distinct().toList();

        List<BatchShipResultVO.Failure> failures = new ArrayList<>();
        int shipped = 0;
        for (int i = 0; i < subOrderIds.size(); i++) {
            Long subOrderId = subOrderIds.get(i);
            SubOrder subOrder = subOrderRepository.findByIdAndShopId(subOrderId, shop.getId()).orElse(null);
            if (subOrder == null) {
                failures.add(new BatchShipResultVO.Failure(subOrderId, null, "子订单不存在"));
                continue;
            }
            try {
                validateShippable(subOrder);
            } catch (BusinessException e) {
                failures.add(new BatchShipResultVO.Failure(subOrderId, subOrder.getSubOrderNo(), e.getMessage()));
                continue;
            }
            // 多单共用同一个运单号时追加序号，避免单号重复
            String trackingNo = request.trackingNo();
            if (StringUtils.hasText(trackingNo) && subOrderIds.size() > 1) {
                trackingNo = trackingNo.trim() + "-" + (i + 1);
            }
            doShip(subOrder, request.shipCompany(), trackingNo);
            shipped++;
        }
        return new BatchShipResultVO(subOrderIds.size(), shipped, failures);
    }

    private void validateShippable(SubOrder subOrder) {
        if (subOrder.getOrder().getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "主订单未支付或已结束，不能发货");
        }
        if (subOrder.getStatus() != SubOrderStatus.WAIT_SHIP) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前子订单状态不可发货");
        }
    }

    /** 写入物流信息：公司不填默认模拟物流，运单号不填自动生成模拟单号 */
    private void doShip(SubOrder subOrder, String shipCompany, String trackingNo) {
        subOrder.setShipCompany(StringUtils.hasText(shipCompany) ? shipCompany.trim() : "模拟物流");
        subOrder.setTrackingNo(StringUtils.hasText(trackingNo)
                ? trackingNo.trim() : "MOCK" + subOrder.getSubOrderNo());
        subOrder.setShippedAt(LocalDateTime.now());
        subOrder.setStatus(SubOrderStatus.SHIPPED);

        notificationService.notify(
                subOrder.getOrder().getUser().getId(),
                NotificationType.ORDER_SHIPPED,
                "商家已发货",
                "子订单 " + subOrder.getSubOrderNo() + " 已发货，物流："
                        + subOrder.getShipCompany() + "，运单号：" + subOrder.getTrackingNo(),
                "/orders/" + subOrder.getOrder().getId());
    }

    private SubOrder requireOwnedSubOrder(Long shopId, Long subOrderId) {
        return subOrderRepository.findByIdAndShopId(subOrderId, shopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "子订单不存在"));
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法查看履约订单"));
    }
}
