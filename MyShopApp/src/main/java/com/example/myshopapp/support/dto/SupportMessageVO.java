package com.example.myshopapp.support.dto;

import com.example.myshopapp.support.entity.SupportMessage;
import com.example.myshopapp.support.entity.SupportSenderType;

import java.time.LocalDateTime;

/**
 * 客服消息视图。
 */
public record SupportMessageVO(
        Long id,
        Long conversationId,
        SupportSenderType senderType,
        Long senderId,
        String content,
        boolean readFlag,
        LocalDateTime createdAt
) {

    public static SupportMessageVO from(SupportMessage message) {
        return new SupportMessageVO(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderType(),
                message.getSenderId(),
                message.getContent(),
                message.isReadFlag(),
                message.getCreatedAt());
    }
}
