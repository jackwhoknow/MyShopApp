package com.example.myshopapp.support;

import com.example.myshopapp.common.api.ApiResponse;
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
 * 商家客服接口：查看本店会话、增量拉取消息、回复买家。
 */
@RestController
@RequestMapping("/api/merchant/support")
@RequiredArgsConstructor
public class MerchantSupportController {

    private final SupportService supportService;

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(supportService.listForShop(loginUser.id()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<SupportMessageVO>> messages(@AuthenticationPrincipal LoginUser loginUser,
                                                        @PathVariable Long id,
                                                        @RequestParam(defaultValue = "0") Long afterId) {
        return ApiResponse.ok(supportService.messagesForShop(loginUser.id(), id, afterId));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<SupportMessageVO> reply(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long id,
                                               @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(supportService.sendByShop(loginUser.id(), id, request));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unread(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(supportService.unreadForShop(loginUser.id()));
    }
}
