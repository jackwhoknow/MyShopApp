package com.example.myshopapp.support;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.notification.entity.NotificationType;
import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.repository.ShopRepository;
import com.example.myshopapp.support.dto.ConversationVO;
import com.example.myshopapp.support.dto.SendMessageRequest;
import com.example.myshopapp.support.dto.StartConversationRequest;
import com.example.myshopapp.support.dto.SupportMessageVO;
import com.example.myshopapp.support.entity.SupportConversation;
import com.example.myshopapp.support.entity.SupportMessage;
import com.example.myshopapp.support.entity.SupportSenderType;
import com.example.myshopapp.support.repository.SupportConversationRepository;
import com.example.myshopapp.support.repository.SupportMessageRepository;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线客服：买家与店铺一对一沟通，前端按 afterId 轮询增量消息。
 * 不引入 WebSocket，部署简单，消息与未读数都落库。
 */
@Service
@RequiredArgsConstructor
public class SupportService {

    private static final int PREVIEW_LENGTH = 200;

    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** 买家发起会话：同一店铺复用已有会话 */
    @Transactional
    public ConversationVO startConversation(Long userId, StartConversationRequest request) {
        Shop shop = shopRepository.findById(request.shopId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在"));
        SupportConversation conversation = conversationRepository
                .findByUserIdAndShopId(userId, shop.getId())
                .orElseGet(() -> {
                    SupportConversation created = new SupportConversation();
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setShop(shop);
                    created.setOrderId(request.orderId());
                    return conversationRepository.save(created);
                });
        if (request.orderId() != null && conversation.getOrderId() == null) {
            conversation.setOrderId(request.orderId());
        }
        return ConversationVO.from(conversation, false);
    }

    @Transactional(readOnly = true)
    public List<ConversationVO> listForUser(Long userId) {
        return conversationRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(item -> ConversationVO.from(item, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationVO> listForShop(Long ownerId) {
        Shop shop = requireShop(ownerId);
        return conversationRepository.findByShopIdOrderByIdDesc(shop.getId()).stream()
                .map(item -> ConversationVO.from(item, true))
                .toList();
    }

    /** 买家拉取消息，同时把店铺消息标记为已读 */
    @Transactional
    public List<SupportMessageVO> messagesForUser(Long userId, Long conversationId, Long afterId) {
        SupportConversation conversation = conversationRepository
                .findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        messageRepository.markRead(conversationId, SupportSenderType.SHOP);
        conversation.setUserUnread(0);
        return loadMessages(conversationId, afterId);
    }

    /** 商家拉取消息，同时把买家消息标记为已读 */
    @Transactional
    public List<SupportMessageVO> messagesForShop(Long ownerId, Long conversationId, Long afterId) {
        Shop shop = requireShop(ownerId);
        SupportConversation conversation = conversationRepository
                .findByIdAndShopId(conversationId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        messageRepository.markRead(conversationId, SupportSenderType.USER);
        conversation.setShopUnread(0);
        return loadMessages(conversationId, afterId);
    }

    @Transactional
    public SupportMessageVO sendByUser(Long userId, Long conversationId, SendMessageRequest request) {
        SupportConversation conversation = conversationRepository
                .findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        SupportMessage message = appendMessage(conversation, SupportSenderType.USER, userId,
                request.content());
        conversation.setShopUnread(conversation.getShopUnread() + 1);
        touch(conversation, request.content());
        return SupportMessageVO.from(message);
    }

    @Transactional
    public SupportMessageVO sendByShop(Long ownerId, Long conversationId, SendMessageRequest request) {
        Shop shop = requireShop(ownerId);
        SupportConversation conversation = conversationRepository
                .findByIdAndShopId(conversationId, shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        SupportMessage message = appendMessage(conversation, SupportSenderType.SHOP, ownerId,
                request.content());
        conversation.setUserUnread(conversation.getUserUnread() + 1);
        touch(conversation, request.content());

        notificationService.notify(conversation.getUser().getId(), NotificationType.SUPPORT_MESSAGE,
                "店铺客服回复了你",
                "「" + shop.getName() + "」客服：" + request.content().trim(),
                "/support");
        return SupportMessageVO.from(message);
    }

    @Transactional(readOnly = true)
    public long unreadForUser(Long userId) {
        return conversationRepository.sumUserUnread(userId);
    }

    @Transactional(readOnly = true)
    public long unreadForShop(Long ownerId) {
        return conversationRepository.sumShopUnread(requireShop(ownerId).getId());
    }

    /** 平台客服：全部会话按最近互动倒序 */
    @Transactional(readOnly = true)
    public PageResult<ConversationVO> listForAdmin(int page, int size) {
        return PageResult.of(conversationRepository
                .findAllByOrderByLastMessageAtDescIdDesc(PageRequest.of(page, size))
                .map(item -> ConversationVO.from(item, false)));
    }

    /** 平台客服拉取消息，同时把买卖双方消息标记为已读 */
    @Transactional
    public List<SupportMessageVO> messagesForAdmin(Long conversationId, Long afterId) {
        requireConversation(conversationId);
        messageRepository.markRead(conversationId, SupportSenderType.USER);
        messageRepository.markRead(conversationId, SupportSenderType.SHOP);
        return loadMessages(conversationId, afterId);
    }

    /** 平台客服介入回复：同时提醒买家与商家，双方未读数各自累加 */
    @Transactional
    public SupportMessageVO sendByPlatform(Long operatorId, Long conversationId,
                                           SendMessageRequest request) {
        SupportConversation conversation = requireConversation(conversationId);
        SupportMessage message = appendMessage(conversation, SupportSenderType.PLATFORM,
                operatorId, request.content());
        conversation.setUserUnread(conversation.getUserUnread() + 1);
        conversation.setShopUnread(conversation.getShopUnread() + 1);
        touch(conversation, request.content());

        notificationService.notify(conversation.getUser().getId(), NotificationType.SUPPORT_MESSAGE,
                "平台客服回复了你",
                "平台客服：" + request.content().trim(),
                "/support");
        return SupportMessageVO.from(message);
    }

    private List<SupportMessageVO> loadMessages(Long conversationId, Long afterId) {
        long cursor = afterId == null ? 0L : afterId;
        return messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, cursor)
                .stream().map(SupportMessageVO::from).toList();
    }

    private SupportMessage appendMessage(SupportConversation conversation, SupportSenderType senderType,
                                         Long senderId, String content) {
        SupportMessage message = new SupportMessage();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContent(content.trim());
        return messageRepository.save(message);
    }

    private void touch(SupportConversation conversation, String content) {
        String trimmed = content.trim();
        conversation.setLastMessagePreview(trimmed.length() > PREVIEW_LENGTH
                ? trimmed.substring(0, PREVIEW_LENGTH) : trimmed);
        conversation.setLastMessageAt(LocalDateTime.now());
    }

    private Shop requireShop(Long ownerId) {
        return shopRepository.findFirstByOwnerIdOrderByIdDesc(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "还没有店铺"));
    }

    private SupportConversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
    }
}
