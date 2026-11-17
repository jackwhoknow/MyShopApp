package com.example.myshopapp.points;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.order.entity.Order;
import com.example.myshopapp.points.dto.PointAccountVO;
import com.example.myshopapp.points.dto.PointQuoteVO;
import com.example.myshopapp.points.dto.PointTransactionVO;
import com.example.myshopapp.points.entity.PointAccount;
import com.example.myshopapp.points.entity.PointTransaction;
import com.example.myshopapp.points.entity.PointTransactionType;
import com.example.myshopapp.points.repository.PointAccountRepository;
import com.example.myshopapp.points.repository.PointTransactionRepository;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 买家积分：100 积分抵 1 元，单笔最多抵扣扣券后应付金额的 50%，
 * 支付成功按实付金额（元，向下取整）返还积分。
 */
@Service
@RequiredArgsConstructor
public class PointService {

    /** 100 积分 = 1 元 */
    private static final BigDecimal POINTS_PER_YUAN = BigDecimal.valueOf(100);
    /** 单笔订单积分最多抵扣应付金额的一半 */
    private static final BigDecimal MAX_DEDUCT_RATIO = new BigDecimal("0.5");
    /** 积分使用步长：必须为 100 的整数倍 */
    private static final int POINT_STEP = 100;

    private final PointAccountRepository pointAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PointAccountVO account(Long userId) {
        return pointAccountRepository.findByUserId(userId)
                .map(account -> toAccountVO(account.getBalance(),
                        account.getTotalEarned(), account.getTotalSpent()))
                .orElseGet(() -> toAccountVO(0, 0, 0));
    }

    private PointAccountVO toAccountVO(int balance, int totalEarned, int totalSpent) {
        MemberLevel level = MemberLevel.of(totalEarned);
        MemberLevel next = level.next();
        return new PointAccountVO(balance, totalEarned, totalSpent,
                level.name(), level.label(),
                next == null ? null : next.label(),
                next == null ? null : Math.max(next.threshold() - totalEarned, 0));
    }

    @Transactional(readOnly = true)
    public PageResult<PointTransactionVO> transactions(Long userId, int page, int size) {
        Page<PointTransaction> result = pointTransactionRepository
                .findByUserIdOrderByIdDesc(userId, PageRequest.of(page, size));
        return PageResult.of(result.map(PointTransactionVO::from));
    }

    /**
     * 结算页试算：amount 为扣券后的应付金额。
     * 可抵扣积分取「账户余额」与「应付金额的 50%」的较小值，并向下取整到 100 的整数倍。
     */
    @Transactional(readOnly = true)
    public PointQuoteVO quote(Long userId, BigDecimal amount) {
        int balance = pointAccountRepository.findByUserId(userId)
                .map(PointAccount::getBalance)
                .orElse(0);
        BigDecimal payable = amount == null || amount.signum() < 0 ? BigDecimal.ZERO : amount;
        int limitByAmount = payable.multiply(MAX_DEDUCT_RATIO)
                .multiply(POINTS_PER_YUAN)
                .intValue();
        int maxPoints = Math.min(balance, limitByAmount) / POINT_STEP * POINT_STEP;
        return new PointQuoteVO(balance, maxPoints, BigDecimal.valueOf(maxPoints).movePointLeft(2));
    }

    /** 下单抵扣：加锁校验余额后扣减并记录流水 */
    @Transactional
    public void deduct(Long userId, int points, Long orderId, String orderNo) {
        if (points <= 0) {
            return;
        }
        PointAccount account = lockAccount(userId);
        if (account.getBalance() < points) {
            throw new BusinessException(ErrorCode.CONFLICT, "积分余额不足");
        }
        change(account, -points, PointTransactionType.ORDER_DEDUCT, orderId, orderNo, "下单抵扣积分");
    }

    /** 支付成功：按实付金额返还积分 */
    @Transactional
    public void earn(Long userId, int points, Long orderId, String orderNo) {
        if (points <= 0) {
            return;
        }
        change(lockAccount(userId), points, PointTransactionType.ORDER_EARN, orderId, orderNo, "支付成功赠送积分");
    }

    /** 待支付订单取消：退回已抵扣积分 */
    @Transactional
    public void returnOnCancel(Long userId, int points, Long orderId, String orderNo) {
        if (points <= 0) {
            return;
        }
        change(lockAccount(userId), points, PointTransactionType.ORDER_CANCEL_RETURN, orderId, orderNo,
                "订单取消退回积分");
    }

    /**
     * 整单退款：退回抵扣积分，并扣回支付时赠送的积分（允许余额为负）。
     * 部分退款不调整积分，由调用方在整单全部取消时触发。
     */
    @Transactional
    public void reverseOnFullRefund(Order order) {
        int used = order.getPointsUsed();
        int earned = order.getPointsEarned();
        if (used <= 0 && earned <= 0) {
            return;
        }
        PointAccount account = lockAccount(order.getUser().getId());
        if (used > 0) {
            change(account, used, PointTransactionType.ORDER_REFUND, order.getId(), order.getOrderNo(),
                    "整单退款退回抵扣积分");
        }
        if (earned > 0) {
            change(account, -earned, PointTransactionType.ORDER_REFUND, order.getId(), order.getOrderNo(),
                    "整单退款扣回赠送积分");
        }
    }

    /** 签到等运营奖励：直接增加积分并记账 */
    @Transactional
    public void reward(Long userId, int points, PointTransactionType type, String remark) {
        if (points <= 0) {
            return;
        }
        change(lockAccount(userId), points, type, null, null, remark);
    }

    /** 积分兑换等消费场景：加锁校验余额后扣减 */
    @Transactional
    public void spend(Long userId, int points, PointTransactionType type, String remark) {
        if (points <= 0) {
            return;
        }
        PointAccount account = lockAccount(userId);
        if (account.getBalance() < points) {
            throw new BusinessException(ErrorCode.CONFLICT, "积分余额不足");
        }
        change(account, -points, type, null, null, remark);
    }

    /** 账户不存在时懒创建，余额从 0 开始 */
    private PointAccount lockAccount(Long userId) {
        return pointAccountRepository.findByUserIdForUpdate(userId).orElseGet(() -> {
            PointAccount account = new PointAccount();
            account.setUser(userRepository.getReferenceById(userId));
            return pointAccountRepository.save(account);
        });
    }

    /** 统一变更余额并落一笔流水，balanceAfter 记录变动后余额 */
    private void change(PointAccount account, int delta, PointTransactionType type,
                        Long orderId, String orderNo, String remark) {
        MemberLevel levelBefore = MemberLevel.of(account.getTotalEarned());
        account.setBalance(account.getBalance() + delta);
        if (delta > 0) {
            account.setTotalEarned(account.getTotalEarned() + delta);
        } else {
            account.setTotalSpent(account.getTotalSpent() - delta);
        }
        pointAccountRepository.save(account);

        MemberLevel levelAfter = MemberLevel.of(account.getTotalEarned());
        if (levelAfter.ordinal() > levelBefore.ordinal()) {
            notificationService.notify(account.getUser().getId(), NotificationType.MEMBER_LEVEL_UP,
                    "会员等级提升",
                    "恭喜你升级为" + levelAfter.label() + "，累计获得积分 "
                            + account.getTotalEarned() + " 分",
                    "/points");
        }

        PointTransaction transaction = new PointTransaction();
        transaction.setUser(account.getUser());
        transaction.setChangeAmount(delta);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setType(type);
        transaction.setOrderId(orderId);
        transaction.setOrderNo(orderNo);
        transaction.setRemark(remark);
        pointTransactionRepository.save(transaction);
    }
}
