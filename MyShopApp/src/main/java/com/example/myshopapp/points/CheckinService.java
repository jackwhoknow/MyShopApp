package com.example.myshopapp.points;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.points.dto.CheckinStatusVO;
import com.example.myshopapp.points.entity.PointTransactionType;
import com.example.myshopapp.points.entity.UserCheckin;
import com.example.myshopapp.points.repository.UserCheckinRepository;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日签到：连续签到奖励递增，断签后从基础奖励重新开始。
 * 奖励积分 = 5 + min(连续天数 - 1, 10)，即连续 11 天及以上每天 15 分。
 */
@Service
@RequiredArgsConstructor
public class CheckinService {

    private static final int BASE_POINTS = 5;
    private static final int MAX_BONUS = 10;
    private static final int HISTORY_DAYS = 60;

    private final UserCheckinRepository userCheckinRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public CheckinStatusVO status(Long userId) {
        LocalDate today = LocalDate.now();
        boolean checkedInToday = userCheckinRepository.existsByUserIdAndCheckinDate(userId, today);
        int consecutive = consecutiveDays(userId, today, checkedInToday);
        int todayPoints = pointsFor(consecutive == 0 ? 1 : consecutive);
        int nextPoints = pointsFor(checkedInToday ? consecutive + 1 : consecutive + 1);
        return new CheckinStatusVO(checkedInToday, consecutive,
                checkedInToday ? pointsFor(consecutive) : 0, nextPoints);
    }

    @Transactional
    public CheckinStatusVO checkin(Long userId) {
        LocalDate today = LocalDate.now();
        if (userCheckinRepository.existsByUserIdAndCheckinDate(userId, today)) {
            throw new BusinessException(ErrorCode.CONFLICT, "今天已经签到过了");
        }

        // 昨天有签到才累计连续天数，否则从 1 开始
        boolean yesterday = userCheckinRepository
                .existsByUserIdAndCheckinDate(userId, today.minusDays(1));
        int streak = yesterday ? consecutiveDays(userId, today, false) + 1 : 1;
        int points = pointsFor(streak);

        UserCheckin checkin = new UserCheckin();
        checkin.setUser(userRepository.getReferenceById(userId));
        checkin.setCheckinDate(today);
        checkin.setPoints(points);
        userCheckinRepository.save(checkin);

        pointService.reward(userId, points, PointTransactionType.CHECKIN,
                "每日签到（连续 " + streak + " 天）");
        notificationService.notify(userId, NotificationType.CHECKIN_REWARD, "签到成功",
                "已连续签到 " + streak + " 天，获得 " + points + " 积分", "/points");

        return new CheckinStatusVO(true, streak, points, pointsFor(streak + 1));
    }

    /** 从今天（或昨日）向前数连续签到天数 */
    private int consecutiveDays(Long userId, LocalDate today, boolean checkedInToday) {
        List<UserCheckin> recent = userCheckinRepository
                .findByUserIdOrderByCheckinDateDesc(userId, PageRequest.of(0, HISTORY_DAYS));
        LocalDate cursor = checkedInToday ? today : today.minusDays(1);
        int days = 0;
        for (UserCheckin checkin : recent) {
            if (checkin.getCheckinDate().equals(cursor)) {
                days++;
                cursor = cursor.minusDays(1);
            } else if (checkin.getCheckinDate().isBefore(cursor)) {
                break;
            }
        }
        return days;
    }

    private int pointsFor(int streak) {
        return BASE_POINTS + Math.min(Math.max(streak - 1, 0), MAX_BONUS);
    }
}
