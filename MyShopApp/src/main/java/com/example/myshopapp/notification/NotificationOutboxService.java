package com.example.myshopapp.notification;

import com.example.myshopapp.notification.entity.NotificationOutbox;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发件箱：定时提醒先入队，统一投递，按 dedupe_key 保证只提醒一次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private static final String PENDING = "PENDING";
    private static final String SENT = "SENT";
    private static final int BATCH_SIZE = 200;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationService notificationService;

    /** 入队：相同 dedupeKey 已存在时直接忽略 */
    @Transactional
    public void enqueue(String dedupeKey, Long userId, NotificationType type,
                        String title, String content, String link) {
        if (userId == null || outboxRepository.existsByDedupeKey(dedupeKey)) {
            return;
        }
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setDedupeKey(dedupeKey);
        outbox.setUserId(userId);
        outbox.setType(type.name());
        outbox.setTitle(title);
        outbox.setContent(content);
        outbox.setLink(link);
        outbox.setStatus(PENDING);
        outboxRepository.save(outbox);
    }

    /** 投递待发送通知，返回本次发送条数 */
    @Transactional
    public int dispatchPending() {
        List<NotificationOutbox> pending = outboxRepository
                .findByStatusOrderByIdAsc(PENDING, PageRequest.of(0, BATCH_SIZE));
        int sent = 0;
        for (NotificationOutbox outbox : pending) {
            try {
                notificationService.notify(outbox.getUserId(),
                        NotificationType.valueOf(outbox.getType()),
                        outbox.getTitle(), outbox.getContent(), outbox.getLink());
                outbox.setStatus(SENT);
                outbox.setSentAt(LocalDateTime.now());
                sent++;
            } catch (Exception e) {
                log.warn("通知 {} 投递失败：{}", outbox.getId(), e.getMessage());
            }
        }
        return sent;
    }
}
