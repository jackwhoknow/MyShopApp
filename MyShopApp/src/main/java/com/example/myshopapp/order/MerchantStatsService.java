package com.example.myshopapp.order;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.dto.MerchantStatsVO;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.OrderItemRepository;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 商家经营看板：今日概况、待办数量、累计销售额、近 N 天趋势与热销商品。
 * 销售口径统一为「已发货 + 已完成」的子订单，按主订单支付时间归日。
 */
@Service
@RequiredArgsConstructor
public class MerchantStatsService {

    /** 计入销售额的子订单状态 */
    private static final List<SubOrderStatus> SALES_STATUSES =
            List.of(SubOrderStatus.SHIPPED, SubOrderStatus.COMPLETED);

    /** 退款中只看主订单仍有效的子订单 */
    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.COMPLETED);

    private static final int MAX_DAYS = 30;
    private static final int TOP_PRODUCT_LIMIT = 5;

    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public MerchantStatsVO overview(Long ownerId, int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "统计天数需在 1-" + MAX_DAYS + " 之间");
        }
        Shop shop = requireShop(ownerId);
        Long shopId = shop.getId();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();

        long todayOrderCount = subOrderRepository.countCreatedBetween(
                shopId, today.atStartOfDay(), endAt, SubOrderStatus.CANCELLED);
        long pendingShipCount = subOrderRepository.countByShopAndStatus(
                shopId, SubOrderStatus.WAIT_SHIP, List.of(OrderStatus.PAID));
        long refundingCount = subOrderRepository.countByShopAndStatus(
                shopId, SubOrderStatus.REFUNDING, ACTIVE_ORDER_STATUSES);
        BigDecimal totalSalesAmount = nullToZero(
                subOrderRepository.sumSalesAmount(shopId, SALES_STATUSES));

        List<MerchantStatsVO.TrendPoint> trend =
                buildTrend(shopId, startDate, days, startAt, endAt);
        BigDecimal todaySalesAmount = trend.isEmpty()
                ? BigDecimal.ZERO
                : trend.get(trend.size() - 1).salesAmount();

        List<MerchantStatsVO.TopProduct> topProducts = orderItemRepository
                .findTopProducts(shopId, SALES_STATUSES, PageRequest.of(0, TOP_PRODUCT_LIMIT))
                .stream()
                .map(item -> new MerchantStatsVO.TopProduct(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity() == null ? 0L : item.getQuantity(),
                        nullToZero(item.getSalesAmount())))
                .toList();

        return new MerchantStatsVO(
                todayOrderCount,
                todaySalesAmount,
                pendingShipCount,
                refundingCount,
                totalSalesAmount,
                days,
                trend,
                topProducts);
    }

    /**
     * 近 N 天趋势：先按天铺满 0，再把支付明细累加进去，保证无数据的天也出现在图上。
     */
    private List<MerchantStatsVO.TrendPoint> buildTrend(Long shopId, LocalDate startDate, int days,
                                                        LocalDateTime startAt, LocalDateTime endAt) {
        long[] orderCounts = new long[days];
        BigDecimal[] salesAmounts = new BigDecimal[days];
        Arrays.fill(salesAmounts, BigDecimal.ZERO);

        for (SubOrderRepository.SalesPointProjection point
                : subOrderRepository.findSalesPoints(shopId, SALES_STATUSES, startAt, endAt)) {
            LocalDateTime paidAt = point.getPaidAt();
            if (paidAt == null) {
                continue;
            }
            int index = (int) (paidAt.toLocalDate().toEpochDay() - startDate.toEpochDay());
            if (index < 0 || index >= days) {
                continue;
            }
            orderCounts[index]++;
            salesAmounts[index] = salesAmounts[index].add(nullToZero(point.getTotalAmount()));
        }

        List<MerchantStatsVO.TrendPoint> trend = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            trend.add(new MerchantStatsVO.TrendPoint(startDate.plusDays(i), orderCounts[i], salesAmounts[i]));
        }
        return trend;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法查看经营看板"));
    }
}
