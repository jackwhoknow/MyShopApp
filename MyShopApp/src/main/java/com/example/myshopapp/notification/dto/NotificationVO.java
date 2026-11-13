package com.example.myshopapp.notification.dto;

import com.example.myshopapp.notification.entity.Notification;

import java.time.LocalDateTime;

/**
 * 站内通知列表项。
 */
public record NotificationVO(
        Long id,
        String type,
        String title,
        String content,
        String link,
        boolean readFlag,
        LocalDateTime createdAt
) {

    public static NotificationVO from(Notification notification) {
        return new NotificationVO(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getContent(),
                notification.getLink(),
                notification.isReadFlag(),
                notification.getCreatedAt());
    }
}
