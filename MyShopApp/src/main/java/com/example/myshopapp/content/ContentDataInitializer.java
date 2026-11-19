package com.example.myshopapp.content;

import com.example.myshopapp.content.entity.Announcement;
import com.example.myshopapp.content.entity.Banner;
import com.example.myshopapp.content.repository.AnnouncementRepository;
import com.example.myshopapp.content.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示内容初始化：只在非生产环境补充轮播与公告，避免生产首页出现假数据。
 * 生产环境的首页内容由运营后台维护。
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class ContentDataInitializer implements CommandLineRunner {

    private final BannerRepository bannerRepository;
    private final AnnouncementRepository announcementRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (bannerRepository.count() == 0) {
            createBanner("新品首发 · 潮流好物抢先看", "/products/1", 0);
            createBanner("限时优惠 · 精选商品低至 5 折", "/products/2", 1);
            createBanner("品质生活 · 从星耀商城开始", null, 2);
            log.info("已初始化示例轮播图");
        }

        if (announcementRepository.count() == 0) {
            createAnnouncement("平台服务升级公告",
                    "为了提供更稳定的购物体验，平台将于本周六 02:00-04:00 进行服务升级，期间可能出现短暂访问异常，请提前安排好购物时间。", true);
            createAnnouncement("双十一活动预告",
                    "11 月 1 日起，全场满 100 减 10，叠加店铺优惠券更划算。活动期间下单的商品将在 48 小时内发货，敬请期待。", false);
            log.info("已初始化示例平台公告");
        }
    }

    private void createBanner(String title, String linkUrl, int sortOrder) {
        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setLinkUrl(linkUrl);
        banner.setSortOrder(sortOrder);
        banner.setEnabled(true);
        bannerRepository.save(banner);
    }

    private void createAnnouncement(String title, String content, boolean pinned) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setPinned(pinned);
        announcement.setEnabled(true);
        announcementRepository.save(announcement);
    }
}
