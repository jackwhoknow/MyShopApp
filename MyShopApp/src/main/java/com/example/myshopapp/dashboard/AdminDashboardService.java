package com.example.myshopapp.dashboard;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.dashboard.dto.AdminDashboardVO;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.OrderItemRepository;
import com.example.myshopapp.order.repository.OrderRepository;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.product.entity.ProductStatus;
import com.example.myshopapp.product.repository.ProductRepository;
import com.example.myshopapp.shop.entity.ShopStatus;
import com.example.myshopapp.shop.repository.ShopRepository;
import com.example.myshopapp.user.repository.UserRepository;
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
 * 平台经营看板：与商家看板保持同一销售口径，
 * 销售额只计入「已发货 + 已完成」的子订单，按主订单支付时间归日。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final List<SubOrderStatus> SALES_STATUSES =
            List.of(SubOrderStatus.SHIPPED, SubOrderStatus.COMPLETED);

    private static final List<OrderStatus> PAID_ORDER_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.COMPLETED);

    private static final int MAX_DAYS = 30;
    private static final int TOP_PRODUCT_LIMIT = 5;

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public AdminDashboardVO overview(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "统计天数需在 1-" + MAX_DAYS + " 之间");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();

        List<AdminDashboardVO.TrendPoint> trend = buildTrend(startDate, days, startAt, endAt);
        BigDecimal todaySalesAmount = trend.isEmpty()
                ? BigDecimal.ZERO
                : trend.get(trend.size() - 1).salesAmount();

        List<AdminDashboardVO.TopProduct> topProducts = orderItemRepository
                .findTopProductsAll(SALES_STATUSES, PageRequest.of(0, TOP_PRODUCT_LIMIT))
                .stream()
                .map(item -> new AdminDashboardVO.TopProduct(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity() == null ? 0L : item.getQuantity(),
                        nullToZero(item.getSalesAmount())))
                .toList();

        return new AdminDashboardVO(
                userRepository.countByDeletedFalse(),
                shopRepository.countByStatus(ShopStatus.ACTIVE),
                shopRepository.countByStatus(ShopStatus.PENDING),
                productRepository.countByStatus(ProductStatus.ON_SHELF),
                orderRepository.countByStatusIn(PAID_ORDER_STATUSES),
                nullToZero(subOrderRepository.sumSalesAmountAll(SALES_STATUSES)),
                subOrderRepository.countCreatedBetweenAll(today.atStartOfDay(), endAt, SubOrderStatus.CANCELLED),
                todaySalesAmount,
                subOrderRepository.countByStatusAll(SubOrderStatus.REFUNDING, PAID_ORDER_STATUSES),
                days,
                trend,
                topProducts);
    }

    /** 近 N 天趋势：先按天铺满 0，再把支付明细累加进去 */
    private List<AdminDashboardVO.TrendPoint> buildTrend(LocalDate startDate, int days,
                                                         LocalDateTime startAt, LocalDateTime endAt) {
        long[] orderCounts = new long[days];
        BigDecimal[] salesAmounts = new BigDecimal[days];
        Arrays.fill(salesAmounts, BigDecimal.ZERO);

        for (SubOrderRepository.SalesPointProjection point
                : subOrderRepository.findSalesPointsAll(SALES_STATUSES, startAt, endAt)) {
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

        List<AdminDashboardVO.TrendPoint> trend = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            trend.add(new AdminDashboardVO.TrendPoint(startDate.plusDays(i), orderCounts[i], salesAmounts[i]));
        }
        return trend;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
