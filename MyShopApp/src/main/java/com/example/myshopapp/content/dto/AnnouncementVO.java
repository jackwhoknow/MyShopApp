package com.example.myshopapp.content.dto;

import com.example.myshopapp.content.entity.Announcement;

import java.time.LocalDateTime;

public record AnnouncementVO(
        Long id,
        String title,
        String content,
        boolean pinned,
        boolean enabled,
        LocalDateTime publishedAt,
        LocalDateTime createdAt) {

    public static AnnouncementVO from(Announcement announcement) {
        return new AnnouncementVO(announcement.getId(), announcement.getTitle(), announcement.getContent(),
                announcement.isPinned(), announcement.isEnabled(), announcement.getPublishedAt(),
                announcement.getCreatedAt());
    }
}
