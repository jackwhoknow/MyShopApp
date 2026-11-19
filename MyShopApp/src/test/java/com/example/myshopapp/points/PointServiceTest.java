package com.example.myshopapp.points;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.points.dto.PointQuoteVO;
import com.example.myshopapp.points.entity.PointAccount;
import com.example.myshopapp.points.entity.PointTransaction;
import com.example.myshopapp.points.repository.PointAccountRepository;
import com.example.myshopapp.points.repository.PointTransactionRepository;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 积分核心规则：抵扣上限、余额校验与流水记账。 */
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private PointAccountRepository pointAccountRepository;
    @Mock
    private PointTransactionRepository pointTransactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("试算抵扣：应付金额的一半为上限，并向下取整到 100 分")
    void quoteCapsDeductionAtHalfAndRoundsDown() {
        when(pointAccountRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(account(10000)));

        PointQuoteVO quote = pointService.quote(USER_ID, new BigDecimal("150.00"));

        assertThat(quote.maxDeductiblePoints()).isEqualTo(7500);
        assertThat(quote.maxDeductibleAmount()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("试算抵扣：账户余额不足时按余额向下取整")
    void quoteUsesBalanceWhenLessThanAmountLimit() {
        when(pointAccountRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(account(250)));

        PointQuoteVO quote = pointService.quote(USER_ID, new BigDecimal("10.00"));

        assertThat(quote.maxDeductiblePoints()).isEqualTo(200);
    }

    @Test
    @DisplayName("下单抵扣：余额不足直接拒绝，不产生流水")
    void deductRejectsInsufficientBalance() {
        when(pointAccountRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(account(100)));

        assertThatThrownBy(() -> pointService.deduct(USER_ID, 200, 1L, "NO1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("余额不足");
    }

    @Test
    @DisplayName("支付返积分：更新余额并写入变动流水")
    void earnUpdatesBalanceAndRecordsTransaction() {
        PointAccount account = account(100);
        when(pointAccountRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(account));

        pointService.earn(USER_ID, 200, 1L, "NO1");

        assertThat(account.getBalance()).isEqualTo(300);
        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getChangeAmount()).isEqualTo(200);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(300);
    }

    private PointAccount account(int balance) {
        PointAccount account = new PointAccount();
        account.setUser(new User());
        account.setBalance(balance);
        return account;
    }
}
