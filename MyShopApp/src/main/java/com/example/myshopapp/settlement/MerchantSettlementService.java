package com.example.myshopapp.settlement;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.order.entity.AfterSaleStatus;
import com.example.myshopapp.order.entity.OrderStatus;
import com.example.myshopapp.order.entity.SubOrder;
import com.example.myshopapp.order.entity.SubOrderStatus;
import com.example.myshopapp.order.repository.AfterSaleRepository;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.settlement.dto.SettlementBillVO;
import com.example.myshopapp.settlement.dto.SettlementSummaryVO;
import com.example.myshopapp.settlement.dto.WithdrawalCreateRequest;
import com.example.myshopapp.settlement.dto.WithdrawalVO;
import com.example.myshopapp.settlement.entity.WithdrawalRequest;
import com.example.myshopapp.settlement.entity.WithdrawalStatus;
import com.example.myshopapp.settlement.repository.WithdrawalRequestRepository;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商家结算：
 * 1. 收入口径为「已发货 + 已完成」且主订单已支付/已完成的子订单，退款子订单不计入；
 * 2. 可提现余额 = 可结算收入 - 已打款提现 - 冻结中的提现申请。
 */
@Service
@RequiredArgsConstructor
public class MerchantSettlementService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 计入可结算收入的子订单状态 */
    private static final List<SubOrderStatus> SETTLEABLE_STATUSES =
            List.of(SubOrderStatus.SHIPPED, SubOrderStatus.COMPLETED);

    /** 冻结金额：待审核 + 已审核待打款 */
    private static final List<WithdrawalStatus> FROZEN_STATUSES =
            List.of(WithdrawalStatus.PENDING, WithdrawalStatus.APPROVED);

    private final SubOrderRepository subOrderRepository;
    private final AfterSaleRepository afterSaleRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public SettlementSummaryVO summary(Long ownerId) {
        return buildSummary(requireShop(ownerId).getId());
    }

    @Transactional(readOnly = true)
    public PageResult<SettlementBillVO> bills(Long ownerId, SubOrderStatus status, String keyword,
                                              int page, int size) {
        Shop shop = requireShop(ownerId);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = normalizedKeyword != null;

        Page<SubOrder> result = subOrderRepository.searchSettlementBills(
                shop.getId(),
                status != null,
                status,
                hasKeyword,
                hasKeyword ? "%" + normalizedKeyword + "%" : null,
                PageRequest.of(page, size));
        return PageResult.of(result.map(this::toBill));
    }

    @Transactional(readOnly = true)
    public PageResult<WithdrawalVO> withdrawals(Long ownerId, int page, int size) {
        Shop shop = requireShop(ownerId);
        return PageResult.of(withdrawalRequestRepository
                .findByShopIdOrderByIdDesc(shop.getId(), PageRequest.of(page, size))
                .map(WithdrawalVO::from));
    }

    /**
     * 发起提现：锁定店铺行后重新计算可提现余额，防止并发重复占用额度。
     */
    @Transactional
    public WithdrawalVO createWithdrawal(Long ownerId, WithdrawalCreateRequest request) {
        Shop shop = requireShop(ownerId);
        shopRepository.findByIdForUpdate(shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在"));

        SettlementSummaryVO summary = buildSummary(shop.getId());
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(summary.availableAmount()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "提现金额不能超过可提现余额 ¥" + summary.availableAmount().toPlainString());
        }

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalNo(generateNo());
        withdrawal.setShop(shop);
        withdrawal.setAmount(amount);
        withdrawal.setAccountName(request.accountName().trim());
        withdrawal.setAccountNo(request.accountNo().trim());
        withdrawal.setBankName(request.bankName().trim());
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        return WithdrawalVO.from(withdrawalRequestRepository.save(withdrawal));
    }

    private SettlementSummaryVO buildSummary(Long shopId) {
        BigDecimal totalIncome = nullToZero(subOrderRepository.sumSettleableAmount(
                shopId, SETTLEABLE_STATUSES, List.of(OrderStatus.PAID, OrderStatus.COMPLETED)));
        BigDecimal refundedAmount = nullToZero(afterSaleRepository
                .sumAmountByShopAndStatus(shopId, AfterSaleStatus.REFUNDED));
        BigDecimal withdrawnAmount = nullToZero(withdrawalRequestRepository
                .sumAmountByShopAndStatuses(shopId, List.of(WithdrawalStatus.PAID)));
        BigDecimal frozenAmount = nullToZero(withdrawalRequestRepository
                .sumAmountByShopAndStatuses(shopId, FROZEN_STATUSES));

        BigDecimal available = totalIncome.subtract(withdrawnAmount).subtract(frozenAmount);
        if (available.signum() < 0) {
            available = BigDecimal.ZERO;
        }
        long pendingCount = withdrawalRequestRepository.countByShopIdAndStatusIn(shopId, FROZEN_STATUSES);
        long totalCount = withdrawalRequestRepository.countByShopId(shopId);

        return new SettlementSummaryVO(totalIncome, refundedAmount, withdrawnAmount, frozenAmount,
                available, pendingCount, totalCount);
    }

    private SettlementBillVO toBill(SubOrder subOrder) {
        boolean settleable = SETTLEABLE_STATUSES.contains(subOrder.getStatus())
                && (subOrder.getOrder().getStatus() == OrderStatus.PAID
                || subOrder.getOrder().getStatus() == OrderStatus.COMPLETED);
        return new SettlementBillVO(
                subOrder.getId(),
                subOrder.getSubOrderNo(),
                subOrder.getOrder().getOrderNo(),
                subOrder.getTotalAmount(),
                subOrder.getStatus().name(),
                subOrder.getOrder().getPaidAt(),
                subOrder.getCompletedAt(),
                settleable);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺，无法查看结算"));
    }

    private String generateNo() {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "WD" + LocalDateTime.now().format(NO_FORMATTER) + random;
    }
}
