package com.example.myshopapp.support.dto;

import com.example.myshopapp.support.entity.SupportConversation;

import java.time.LocalDateTime;

/**
 * 客服会话列表项：forShop 决定未读数取买家未读还是商家未读。
 */
public record ConversationVO(
        Long id,
        Long userId,
        String userName,
        Long shopId,
        String shopName,
        Long orderId,
        String status,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        int unread
) {

    public static ConversationVO from(SupportConversation conversation, boolean forShop) {
        return new ConversationVO(
                conversation.getId(),
                conversation.getUser().getId(),
                conversation.getUser().getNickname() == null
                        ? conversation.getUser().getUsername() : conversation.getUser().getNickname(),
                conversation.getShop().getId(),
                conversation.getShop().getName(),
                conversation.getOrderId(),
                conversation.getStatus(),
                conversation.getLastMessagePreview(),
                conversation.getLastMessageAt(),
                forShop ? conversation.getShopUnread() : conversation.getUserUnread());
    }
}
