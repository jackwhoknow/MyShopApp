package com.example.myshopapp.content;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.content.dto.AnnouncementVO;
import com.example.myshopapp.content.dto.HomeContentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台首页内容接口，游客可访问。
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/home")
    public ApiResponse<HomeContentVO> home() {
        return ApiResponse.ok(contentService.home());
    }

    @GetMapping("/announcements/{id}")
    public ApiResponse<AnnouncementVO> announcement(@PathVariable Long id) {
        return ApiResponse.ok(contentService.announcement(id));
    }
}
