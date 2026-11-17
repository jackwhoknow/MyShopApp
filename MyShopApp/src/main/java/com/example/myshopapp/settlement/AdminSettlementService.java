package com.example.myshopapp.settlement;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.settlement.dto.AdminSettlementSummaryVO;
import com.example.myshopapp.settlement.dto.WithdrawalVO;
import com.example.myshopapp.settlement.entity.WithdrawalRequest;
import com.example.myshopapp.settlement.entity.WithdrawalStatus;
import com.example.myshopapp.settlement.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台提现审核：审核通过后打款，驳回时释放冻结金额并通知商家。
 */
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public AdminSettlementSummaryVO summary() {
        return new AdminSettlementSummaryVO(
                withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING),
                nullToZero(withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PENDING)),
                withdrawalRequestRepository.countByStatus(WithdrawalStatus.APPROVED),
                nullToZero(withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.APPROVED)),
                withdrawalRequestRepository.countByStatus(WithdrawalStatus.PAID),
                nullToZero(withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PAID)));
    }

    @Transactional(readOnly = true)
    public PageResult<WithdrawalVO> list(WithdrawalStatus status, String keyword, int page, int size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = normalizedKeyword != null;
        return PageResult.of(withdrawalRequestRepository.searchAdmin(
                        status != null,
                        status,
                        hasKeyword,
                        hasKeyword ? "%" + normalizedKeyword + "%" : null,
                        PageRequest.of(page, size))
                .map(WithdrawalVO::from));
    }

    /** 审核通过：进入待打款状态，金额继续冻结 */
    @Transactional
    public WithdrawalVO approve(Long id, String remark) {
        WithdrawalRequest withdrawal = requireForUpdate(id);
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "该提现申请不在待审核状态");
        }
        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawal.setReviewedAt(LocalDateTime.now());
        withdrawal.setAdminRemark(trimToNull(remark));

        notifyOwner(withdrawal, NotificationType.WITHDRAWAL_APPROVED, "提现审核通过",
                "提现单 " + withdrawal.getWithdrawalNo() + " 已审核通过，等待平台打款");
        return WithdrawalVO.from(withdrawal);
    }

    /** 驳回：金额解冻回到可提现余额 */
    @Transactional
    public WithdrawalVO reject(Long id, String remark) {
        WithdrawalRequest withdrawal = requireForUpdate(id);
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING
                && withdrawal.getStatus() != WithdrawalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "该提现申请已打款或已驳回");
        }
        String reason = trimToNull(remark);
        if (reason == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写驳回原因");
        }
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setReviewedAt(LocalDateTime.now());
        withdrawal.setAdminRemark(reason);

        notifyOwner(withdrawal, NotificationType.WITHDRAWAL_REJECTED, "提现申请被驳回",
                "提现单 " + withdrawal.getWithdrawalNo() + " 被驳回：" + reason);
        return WithdrawalVO.from(withdrawal);
    }

    /** 打款：仅审核通过的提现单可操作，打款后计入已提现 */
    @Transactional
    public WithdrawalVO pay(Long id, String remark) {
        WithdrawalRequest withdrawal = requireForUpdate(id);
        if (withdrawal.getStatus() != WithdrawalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有审核通过的提现申请可以打款");
        }
        withdrawal.setStatus(WithdrawalStatus.PAID);
        withdrawal.setPaidAt(LocalDateTime.now());
        String note = trimToNull(remark);
        if (note != null) {
            withdrawal.setAdminRemark(note);
        }

        notifyOwner(withdrawal, NotificationType.WITHDRAWAL_PAID, "提现已打款",
                "提现单 " + withdrawal.getWithdrawalNo() + " 已打款 ¥"
                        + withdrawal.getAmount().toPlainString() + "，请注意查收");
        return WithdrawalVO.from(withdrawal);
    }

    private WithdrawalRequest requireForUpdate(Long id) {
        return withdrawalRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "提现申请不存在"));
    }

    private void notifyOwner(WithdrawalRequest withdrawal, NotificationType type,
                             String title, String content) {
        notificationService.notify(
                withdrawal.getShop().getOwner().getId(),
                type,
                title,
                content,
                "/merchant/settlement");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
