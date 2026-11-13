package com.example.myshopapp.notification;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.dto.AdminBroadcastRequest;
import com.example.myshopapp.notification.dto.AdminBroadcastVO;
import com.example.myshopapp.notification.dto.AudienceCountsVO;
import com.example.myshopapp.notification.entity.Notification;
import com.example.myshopapp.notification.entity.NotificationAudience;
import com.example.myshopapp.notification.entity.NotificationBroadcast;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.notification.repository.NotificationBroadcastRepository;
import com.example.myshopapp.notification.repository.NotificationRepository;
import com.example.myshopapp.user.RoleCode;
import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.entity.UserStatus;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员通知群发：按人群写入站内通知，并保留发送记录。
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationBroadcastRepository broadcastRepository;

    @Transactional
    public AdminBroadcastVO broadcast(Long adminId, String adminName, AdminBroadcastRequest request) {
        String title = request.title().trim();
        String content = request.content().trim();
        String link = StringUtils.hasText(request.link()) ? request.link().trim() : null;
        if (link != null && !link.startsWith("/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "跳转地址需以 / 开头");
        }

        String role = roleOf(request.audience());
        List<User> recipients = userRepository.findActiveRecipients(UserStatus.ACTIVE, role);

        List<Notification> notifications = new ArrayList<>(recipients.size());
        for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.setUser(recipient);
            notification.setType(NotificationType.ADMIN_BROADCAST);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setLink(link);
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);

        NotificationBroadcast record = new NotificationBroadcast();
        record.setTitle(title);
        record.setContent(content);
        record.setLink(link);
        record.setAudience(request.audience());
        record.setRecipientCount(recipients.size());
        record.setSenderName(adminName);
        return AdminBroadcastVO.from(broadcastRepository.save(record));
    }

    @Transactional(readOnly = true)
    public PageResult<AdminBroadcastVO> history(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResult.of(broadcastRepository.findAllByOrderByIdDesc(pageable).map(AdminBroadcastVO::from));
    }

    @Transactional(readOnly = true)
    public AudienceCountsVO audienceCounts() {
        return new AudienceCountsVO(
                userRepository.countActiveRecipients(UserStatus.ACTIVE, ""),
                userRepository.countActiveRecipients(UserStatus.ACTIVE, RoleCode.BUYER),
                userRepository.countActiveRecipients(UserStatus.ACTIVE, RoleCode.MERCHANT));
    }

    /** 目标人群映射到角色编码，ALL 表示不限角色 */
    private String roleOf(NotificationAudience audience) {
        return switch (audience) {
            case ALL -> "";
            case BUYER -> RoleCode.BUYER;
            case MERCHANT -> RoleCode.MERCHANT;
        };
    }
}
