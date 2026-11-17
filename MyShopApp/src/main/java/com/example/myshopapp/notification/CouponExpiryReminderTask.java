package com.example.myshopapp.notification;

import com.example.myshopapp.coupon.entity.UserCoupon;
import com.example.myshopapp.coupon.entity.UserCouponStatus;
import com.example.myshopapp.coupon.repository.UserCouponRepository;
import com.example.myshopapp.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 优惠券到期提醒：扫描即将到期且未使用的券，经发件箱去重后提醒买家。
 * 扫描间隔默认 1 小时，dedupe_key 保证每张券只提醒一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpiryReminderTask {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserCouponRepository userCouponRepository;
    private final NotificationOutboxService outboxService;

    @Value("${myshop.coupon.expiry-warn-days:3}")
    private long warnDays;

    @Scheduled(fixedDelayString = "${myshop.coupon.expiry-scan-ms:3600000}",
            initialDelayString = "${myshop.coupon.expiry-scan-ms:3600000}")
    @SchedulerLock(name = "couponExpiryReminder", lockAtMostFor = "PT30M", lockAtLeastFor = "PT10S")
    public void remindExpiringCoupons() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusDays(warnDays);
        List<UserCoupon> expiring = userCouponRepository
                .findExpiring(UserCouponStatus.UNUSED, now, deadline);
        if (expiring.isEmpty()) {
            return;
        }

        for (UserCoupon userCoupon : expiring) {
            outboxService.enqueue(
                    "coupon-expiring:" + userCoupon.getId(),
                    userCoupon.getUser().getId(),
                    NotificationType.COUPON_EXPIRING,
                    "优惠券即将到期",
                    "「" + userCoupon.getCoupon().getTitle() + "」将于 "
                            + userCoupon.getCoupon().getEndAt().format(DATE_FORMATTER) + " 到期，请尽快使用",
                    "/coupons");
        }
        int sent = outboxService.dispatchPending();
        log.info("优惠券到期提醒：待提醒 {} 张，已发送 {} 条", expiring.size(), sent);
    }
}
