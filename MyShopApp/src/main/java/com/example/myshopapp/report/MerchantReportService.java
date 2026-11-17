package com.example.myshopapp.report;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.entity.OrderItem;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 商家运营报表导出：订单明细与结算账单 CSV，带 UTF-8 BOM 便于 Excel 直接打开。
 */
@Service
@RequiredArgsConstructor
public class MerchantReportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BOM = "\uFEFF";

    private final SubOrderRepository subOrderRepository;
    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public byte[] exportOrders(Long ownerId, LocalDate startDate, LocalDate endDate) {
        Shop shop = requireShop(ownerId);
        LocalDate[] range = normalizeRange(startDate, endDate);
        List<SubOrder> subOrders = subOrderRepository.findForExport(shop.getId(),
                range[0].atStartOfDay(), range[1].plusDays(1).atStartOfDay());

        StringBuilder csv = new StringBuilder(BOM);
        csv.append("子订单号,主订单号,下单时间,支付时间,状态,商品,规格,数量,单价,实付金额,收货人,手机号\n");
        for (SubOrder subOrder : subOrders) {
            for (OrderItem item : subOrder.getItems()) {
                appendRow(csv, subOrder.getSubOrderNo(), subOrder.getOrder().getOrderNo(),
                        format(subOrder.getCreatedAt()), format(subOrder.getOrder().getPaidAt()),
                        subOrder.getStatus().name(), item.getProductName(),
                        item.getSkuDesc() == null ? "" : item.getSkuDesc(),
                        String.valueOf(item.getQuantity()), item.getPrice().toPlainString(),
                        item.getTotalPrice().toPlainString(),
                        subOrder.getOrder().getReceiverName(), subOrder.getOrder().getReceiverPhone());
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportSettlements(Long ownerId, LocalDate startDate, LocalDate endDate) {
        Shop shop = requireShop(ownerId);
        LocalDate[] range = normalizeRange(startDate, endDate);
        List<SubOrder> subOrders = subOrderRepository.findPaidForExport(shop.getId(),
                range[0].atStartOfDay(), range[1].plusDays(1).atStartOfDay());

        StringBuilder csv = new StringBuilder(BOM);
        csv.append("子订单号,主订单号,支付时间,完成时间,状态,商品金额,优惠金额,满减金额,实付金额\n");
        BigDecimal total = BigDecimal.ZERO;
        for (SubOrder subOrder : subOrders) {
            BigDecimal discount = subOrder.getDiscountAmount() == null
                    ? BigDecimal.ZERO : subOrder.getDiscountAmount();
            BigDecimal promotion = subOrder.getPromotionDiscount() == null
                    ? BigDecimal.ZERO : subOrder.getPromotionDiscount();
            BigDecimal gross = subOrder.getTotalAmount().add(discount).add(promotion);
            total = total.add(subOrder.getTotalAmount());
            appendRow(csv, subOrder.getSubOrderNo(), subOrder.getOrder().getOrderNo(),
                    format(subOrder.getOrder().getPaidAt()), format(subOrder.getCompletedAt()),
                    subOrder.getStatus().name(), gross.toPlainString(), discount.toPlainString(),
                    promotion.toPlainString(), subOrder.getTotalAmount().toPlainString());
        }
        csv.append("合计,,,,,,,,").append(total.toPlainString()).append('\n');
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 未传日期时默认导出最近 30 天 */
    private LocalDate[] normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(29) : startDate;
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        return new LocalDate[]{start, end};
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

    private String escape(String value) {
        String text = value == null ? "" : value;
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺"));
    }
}
