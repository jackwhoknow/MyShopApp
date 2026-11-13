package com.example.myshopapp.content;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.content.dto.AnnouncementRequest;
import com.example.myshopapp.content.dto.AnnouncementVO;
import com.example.myshopapp.content.dto.BannerRequest;
import com.example.myshopapp.content.dto.BannerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台端内容管理接口，需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentService contentService;

    // ==================== 轮播图 ====================

    @GetMapping("/banners")
    public ApiResponse<List<BannerVO>> banners() {
        return ApiResponse.ok(contentService.adminBanners());
    }

    @PostMapping("/banners")
    public ApiResponse<BannerVO> createBanner(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok(contentService.createBanner(request));
    }

    @PutMapping("/banners/{id}")
    public ApiResponse<BannerVO> updateBanner(@PathVariable Long id,
                                              @Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok(contentService.updateBanner(id, request));
    }

    @DeleteMapping("/banners/{id}")
    public ApiResponse<Void> deleteBanner(@PathVariable Long id) {
        contentService.deleteBanner(id);
        return ApiResponse.ok();
    }

    // ==================== 公告 ====================

    @GetMapping("/announcements")
    public ApiResponse<PageResult<AnnouncementVO>> announcements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(contentService.adminAnnouncements(page, size));
    }

    @PostMapping("/announcements")
    public ApiResponse<AnnouncementVO> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(contentService.createAnnouncement(request));
    }

    @PutMapping("/announcements/{id}")
    public ApiResponse<AnnouncementVO> updateAnnouncement(@PathVariable Long id,
                                                          @Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(contentService.updateAnnouncement(id, request));
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        contentService.deleteAnnouncement(id);
        return ApiResponse.ok();
    }
}
