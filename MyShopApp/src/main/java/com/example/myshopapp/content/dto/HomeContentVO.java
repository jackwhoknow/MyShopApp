package com.example.myshopapp.content.dto;

import java.util.List;

/**
 * 首页内容聚合：启用的轮播图 + 最新公告。
 */
public record HomeContentVO(List<BannerVO> banners, List<AnnouncementVO> announcements) {
}
