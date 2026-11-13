package com.example.myshopapp.notification;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.dto.NotificationVO;
import com.example.myshopapp.notification.entity.Notification;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.notification.repository.NotificationRepository;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内通知：业务侧写入通知，用户侧查看与标记已读。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 写入一条通知。调用方都在业务事务内，这里与业务操作同成同败，
     * 避免出现「业务失败但消息已发出」或反之。
     */
    @Transactional
    public void notify(Long userId, NotificationType type, String title, String content, String link) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResult<NotificationVO> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResult.of(notificationRepository.findByUserIdOrderByIdDesc(userId, pageable)
                .map(NotificationVO::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    /** 标记单条已读：重复标记按成功处理 */
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "通知不存在"));
        if (!notification.isReadFlag()) {
            notification.setReadFlag(true);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }
}
