package com.example.myshopapp.support.repository;

import com.example.myshopapp.support.entity.SupportConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, Long> {

    Optional<SupportConversation> findByUserIdAndShopId(Long userId, Long shopId);

    Optional<SupportConversation> findByIdAndUserId(Long id, Long userId);

    Optional<SupportConversation> findByIdAndShopId(Long id, Long shopId);

    List<SupportConversation> findByUserIdOrderByIdDesc(Long userId);

    List<SupportConversation> findByShopIdOrderByIdDesc(Long shopId);

    /** 平台客服视角：按最后消息时间倒序，最近有互动的会话排前面 */
    Page<SupportConversation> findAllByOrderByLastMessageAtDescIdDesc(Pageable pageable);

    @Query("select coalesce(sum(c.userUnread), 0) from SupportConversation c where c.user.id = :userId")
    long sumUserUnread(Long userId);

    @Query("select coalesce(sum(c.shopUnread), 0) from SupportConversation c where c.shop.id = :shopId")
    long sumShopUnread(Long shopId);
}
