package com.example.myshopapp.support;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.support.dto.ConversationVO;
import com.example.myshopapp.support.dto.SendMessageRequest;
import com.example.myshopapp.support.dto.SupportMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台客服接口：跨店铺查看全部会话，平台可介入回复买卖双方。
 */
@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping("/conversations")
    public ApiResponse<PageResult<ConversationVO>> list(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(supportService.listForAdmin(page, size));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<SupportMessageVO>> messages(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "0") Long afterId) {
        return ApiResponse.ok(supportService.messagesForAdmin(id, afterId));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<SupportMessageVO> reply(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long id,
                                               @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(supportService.sendByPlatform(loginUser.id(), id, request));
    }
}
