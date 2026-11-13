package com.example.myshopapp.notification.repository;

import com.example.myshopapp.notification.entity.NotificationBroadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationBroadcastRepository extends JpaRepository<NotificationBroadcast, Long> {

    /** 群发历史：新的在前 */
    Page<NotificationBroadcast> findAllByOrderByIdDesc(Pageable pageable);
}
