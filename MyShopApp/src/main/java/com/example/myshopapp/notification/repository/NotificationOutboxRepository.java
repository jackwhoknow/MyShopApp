package com.example.myshopapp.notification.repository;

import com.example.myshopapp.notification.entity.NotificationOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    List<NotificationOutbox> findByStatusOrderByIdAsc(String status, Pageable pageable);
}
