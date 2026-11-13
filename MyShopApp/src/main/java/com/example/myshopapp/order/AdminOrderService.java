package com.example.myshopapp.order;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.common.util.MaskUtil;
import com.example.myshopapp.order.dto.AdminOrderDetailVO;
import com.example.myshopapp.order.dto.AdminOrderVO;
import com.example.myshopapp.order.entity.AfterSale;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.repository.AfterSaleRepository;
import com.example.myshopapp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台订单监管：管理员只读查看全量订单及详情，不做改单操作。
 */
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 单次导出上限，避免一次性拉取过多数据拖垮服务 */
    private static final int EXPORT_LIMIT = 10000;

    private final OrderRepository orderRepository;
    private final AfterSaleRepository afterSaleRepository;

    @Transactional(readOnly = true)
    public PageResult<AdminOrderVO> list(OrderStatus status, String keyword,
                                         LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = normalizedKeyword != null;
        LocalDateTime startAt = startDate != null ? startDate.atStartOfDay() : null;
        // 结束日期按“当天 23:59:59”处理，即次日 0 点作为开区间上界
        LocalDateTime endAt = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        Page<Order> orders = orderRepository.searchAdmin(
                status != null,
                status,
                hasKeyword,
                hasKeyword ? "%" + normalizedKeyword + "%" : null,
                startAt != null,
                startAt,
                endAt != null,
                endAt,
                pageable);

        Map<Long, AfterSale> latestAfterSales = latestAfterSales(orders.getContent());
        List<AdminOrderVO> list = orders.getContent().stream()
                .map(order -> AdminOrderVO.from(order, latestAfterSales))
                .toList();
        return PageResult.of(list, orders.getTotalElements(), orders.getNumber(), orders.getSize());
    }

    /** 导出订单 CSV：按子订单逐行输出，手机号脱敏 */
    @Transactional(readOnly = true)
    public String exportCsv(OrderStatus status, String keyword, LocalDate startDate, LocalDate endDate) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = normalizedKeyword != null;
        LocalDateTime startAt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endAt = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        Page<Order> orders = orderRepository.searchAdmin(
                status != null, status, hasKeyword,
                hasKeyword ? "%" + normalizedKeyword + "%" : null,
                startAt != null, startAt, endAt != null, endAt,
                PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "id")));

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("订单号,创建时间,支付时间,订单状态,订单金额,买家,收货人,收货手机,收货地址,店铺,子订单号,子订单状态,商品明细,物流公司,物流单号\n");
        for (Order order : orders.getContent()) {
            for (SubOrder subOrder : order.getSubOrders()) {
                String items = subOrder.getItems().stream()
                        .map(item -> item.getProductName()
                                + (item.getSkuDesc() == null ? "" : "(" + item.getSkuDesc() + ")")
                                + "x" + item.getQuantity())
                        .collect(Collectors.joining("; "));
                appendRow(csv,
                        order.getOrderNo(),
                        formatTime(order.getCreatedAt()),
                        formatTime(order.getPaidAt()),
                        order.getStatus().name(),
                        order.getTotalAmount().toPlainString(),
                        order.getUser().getUsername(),
                        order.getReceiverName(),
                        MaskUtil.maskPhone(order.getReceiverPhone()),
                        order.getReceiverAddress(),
                        subOrder.getShop().getName(),
                        subOrder.getSubOrderNo(),
                        subOrder.getStatus().name(),
                        items,
                        subOrder.getShipCompany(),
                        subOrder.getTrackingNo());
            }
        }
        return csv.toString();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(CSV_TIME);
    }

    private void appendRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values[i]));
        }
        csv.append('\n');
    }

    /** CSV 转义：包含逗号、引号、换行时用双引号包裹 */
    private String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needQuote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailVO detail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        return AdminOrderDetailVO.from(order, latestAfterSales(List.of(order)));
    }

    /** 批量查询子订单的最新售后单：结果按 id 倒序，每个子订单取第一条 */
    private Map<Long, AfterSale> latestAfterSales(List<Order> orders) {
        List<Long> subOrderIds = orders.stream()
                .flatMap(order -> order.getSubOrders().stream())
                .map(SubOrder::getId)
                .toList();
        if (subOrderIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, AfterSale> latest = new HashMap<>();
        for (AfterSale afterSale : afterSaleRepository.findBySubOrderIdInOrderByIdDesc(subOrderIds)) {
            latest.putIfAbsent(afterSale.getSubOrder().getId(), afterSale);
        }
        return latest;
    }
}
