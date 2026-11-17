package com.example.myshopapp.support.repository;

import com.example.myshopapp.support.entity.SupportMessage;
import com.example.myshopapp.support.entity.SupportSenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    /** 轮询增量消息：afterId 为已拉取到的最大消息 id */
    List<SupportMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId);

    /** 拉取消息后把对方发来的消息标记为已读 */
    @Modifying
    @Query("""
            update SupportMessage m set m.readFlag = true
            where m.conversation.id = :conversationId
              and m.senderType = :senderType
              and m.readFlag = false
            """)
    int markRead(@Param("conversationId") Long conversationId,
                 @Param("senderType") SupportSenderType senderType);
}
