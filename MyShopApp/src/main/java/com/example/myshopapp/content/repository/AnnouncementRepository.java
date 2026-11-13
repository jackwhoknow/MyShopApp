package com.example.myshopapp.content.repository;

import com.example.myshopapp.content.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** 首页公告栏：启用中的最新 5 条，置顶优先 */
    List<Announcement> findTop5ByEnabledTrueOrderByPinnedDescPublishedAtDescIdDesc();
}
