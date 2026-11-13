package com.example.myshopapp.content;

import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.content.dto.AnnouncementRequest;
import com.example.myshopapp.content.dto.AnnouncementVO;
import com.example.myshopapp.content.dto.BannerRequest;
import com.example.myshopapp.content.dto.BannerVO;
import com.example.myshopapp.content.dto.HomeContentVO;
import com.example.myshopapp.content.entity.Announcement;
import com.example.myshopapp.content.entity.Banner;
import com.example.myshopapp.content.repository.AnnouncementRepository;
import com.example.myshopapp.content.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页内容与平台公告：前台读取启用数据，后台维护轮播与公告。
 */
@Service
@RequiredArgsConstructor
public class ContentService {

    private final BannerRepository bannerRepository;
    private final AnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public HomeContentVO home() {
        List<BannerVO> banners = bannerRepository.findByEnabledTrueOrderBySortOrderAscIdAsc().stream()
                .map(BannerVO::from)
                .toList();
        List<AnnouncementVO> announcements = announcementRepository
                .findTop5ByEnabledTrueOrderByPinnedDescPublishedAtDescIdDesc().stream()
                .map(AnnouncementVO::from)
                .toList();
        return new HomeContentVO(banners, announcements);
    }

    @Transactional(readOnly = true)
    public AnnouncementVO announcement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .filter(Announcement::isEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "公告不存在或已下线"));
        return AnnouncementVO.from(announcement);
    }

    // ==================== 轮播图管理 ====================

    @Transactional(readOnly = true)
    public List<BannerVO> adminBanners() {
        return bannerRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(BannerVO::from).toList();
    }

    @Transactional
    public BannerVO createBanner(BannerRequest request) {
        Banner banner = new Banner();
        applyBanner(banner, request);
        return BannerVO.from(bannerRepository.save(banner));
    }

    @Transactional
    public BannerVO updateBanner(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在"));
        applyBanner(banner, request);
        return BannerVO.from(bannerRepository.save(banner));
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在"));
        bannerRepository.delete(banner);
    }

    private void applyBanner(Banner banner, BannerRequest request) {
        banner.setTitle(request.title().trim());
        banner.setImageUrl(blankToNull(request.imageUrl()));
        banner.setLinkUrl(blankToNull(request.linkUrl()));
        banner.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        banner.setEnabled(request.enabled() == null || request.enabled());
    }

    // ==================== 公告管理 ====================

    @Transactional(readOnly = true)
    public PageResult<AnnouncementVO> adminAnnouncements(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("pinned"),
                Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
        Page<Announcement> result = announcementRepository.findAll(pageable);
        return PageResult.of(result.map(AnnouncementVO::from));
    }

    @Transactional
    public AnnouncementVO createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setPublishedAt(LocalDateTime.now());
        applyAnnouncement(announcement, request);
        return AnnouncementVO.from(announcementRepository.save(announcement));
    }

    @Transactional
    public AnnouncementVO updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "公告不存在"));
        applyAnnouncement(announcement, request);
        return AnnouncementVO.from(announcementRepository.save(announcement));
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "公告不存在"));
        announcementRepository.delete(announcement);
    }

    private void applyAnnouncement(Announcement announcement, AnnouncementRequest request) {
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setPinned(request.pinned() != null && request.pinned());
        announcement.setEnabled(request.enabled() == null || request.enabled());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
