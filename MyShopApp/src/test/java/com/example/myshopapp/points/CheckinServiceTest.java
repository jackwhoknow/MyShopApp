package com.example.myshopapp.points;

import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.points.dto.CheckinStatusVO;
import com.example.myshopapp.points.entity.PointTransactionType;
import com.example.myshopapp.points.entity.UserCheckin;
import com.example.myshopapp.points.repository.UserCheckinRepository;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 签到奖励规则：基础 5 分，连续签到每天 +1，单日封顶 15 分。
 */
@ExtendWith(MockitoExtension.class)
class CheckinServiceTest {

    private static final Long USER_ID = 9L;

    @Mock
    private UserCheckinRepository userCheckinRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PointService pointService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CheckinService checkinService;

    @Test
    @DisplayName("连续第 3 天签到获得 7 积分，并写入签到记录与奖励流水")
    void checkinAwardsStreakBonus() {
        LocalDate today = LocalDate.now();
        when(userCheckinRepository.existsByUserIdAndCheckinDate(USER_ID, today)).thenReturn(false);
        when(userCheckinRepository.existsByUserIdAndCheckinDate(USER_ID, today.minusDays(1)))
                .thenReturn(true);
        when(userCheckinRepository.findByUserIdOrderByCheckinDateDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(List.of(checkin(today.minusDays(1)), checkin(today.minusDays(2))));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(userCheckinRepository.save(any(UserCheckin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckinStatusVO status = checkinService.checkin(USER_ID);

        assertThat(status.checkedInToday()).isTrue();
        assertThat(status.consecutiveDays()).isEqualTo(3);
        assertThat(status.todayPoints()).isEqualTo(7);
        assertThat(status.nextPoints()).isEqualTo(8);

        verify(pointService).reward(eq(USER_ID), eq(7), eq(PointTransactionType.CHECKIN),
                contains("连续 3 天"));
        verify(notificationService).notify(eq(USER_ID), any(), any(), contains("7 积分"), any());
    }

    @Test
    @DisplayName("断签后从第 1 天重新计算，签到奖励回落为基础 5 分")
    void checkinResetsAfterBreak() {
        LocalDate today = LocalDate.now();
        when(userCheckinRepository.existsByUserIdAndCheckinDate(USER_ID, today)).thenReturn(false);
        when(userCheckinRepository.existsByUserIdAndCheckinDate(USER_ID, today.minusDays(1)))
                .thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(userCheckinRepository.save(any(UserCheckin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckinStatusVO status = checkinService.checkin(USER_ID);

        assertThat(status.consecutiveDays()).isEqualTo(1);
        assertThat(status.todayPoints()).isEqualTo(5);
        verify(pointService).reward(eq(USER_ID), eq(5), eq(PointTransactionType.CHECKIN), any());
        // 断签时不需要查询历史记录
        verify(userCheckinRepository, never())
                .findByUserIdOrderByCheckinDateDesc(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("每日签到奖励封顶 15 分")
    void checkinBonusIsCapped() {
        LocalDate today = LocalDate.now();
        when(userCheckinRepository.existsByUserIdAndCheckinDate(USER_ID, today)).thenReturn(false);
        when(userCheckinRepository.existsByUserIdAndCheckinDate(USER_ID, today.minusDays(1)))
                .thenReturn(true);
        // 连续 20 天：奖励应为 5 + min(20, 10) = 15
        List<UserCheckin> history = java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(offset -> checkin(today.minusDays(offset)))
                .toList();
        when(userCheckinRepository.findByUserIdOrderByCheckinDateDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(history);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User());
        when(userCheckinRepository.save(any(UserCheckin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckinStatusVO status = checkinService.checkin(USER_ID);

        assertThat(status.consecutiveDays()).isEqualTo(21);
        assertThat(status.todayPoints()).isEqualTo(15);

        ArgumentCaptor<Integer> points = ArgumentCaptor.forClass(Integer.class);
        verify(pointService).reward(eq(USER_ID), points.capture(), eq(PointTransactionType.CHECKIN), any());
        assertThat(points.getValue()).isEqualTo(15);
    }

    private UserCheckin checkin(LocalDate date) {
        UserCheckin record = new UserCheckin();
        record.setCheckinDate(date);
        record.setPoints(5);
        return record;
    }
}
