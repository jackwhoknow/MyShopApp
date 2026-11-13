package com.example.myshopapp.notification.repository;

import com.example.myshopapp.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 我的通知：新的在前 */
    Page<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFlagFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /** 全部已读：一条 SQL 批量更新，避免逐条写库 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n set n.readFlag = true
            where n.user.id = :userId and n.readFlag = false
            """)
    int markAllRead(@Param("userId") Long userId);
}
