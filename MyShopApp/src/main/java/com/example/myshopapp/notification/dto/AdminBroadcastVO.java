package com.example.myshopapp.notification.dto;

import com.example.myshopapp.notification.entity.NotificationBroadcast;

import java.time.LocalDateTime;

/**
 * 管理员群发记录视图。
 */
public record AdminBroadcastVO(
        Long id,
        String title,
        String content,
        String link,
        String audience,
        int recipientCount,
        String senderName,
        LocalDateTime createdAt
) {

    public static AdminBroadcastVO from(NotificationBroadcast broadcast) {
        return new AdminBroadcastVO(
                broadcast.getId(),
                broadcast.getTitle(),
                broadcast.getContent(),
                broadcast.getLink(),
                broadcast.getAudience().name(),
                broadcast.getRecipientCount(),
                broadcast.getSenderName(),
                broadcast.getCreatedAt());
    }
}
