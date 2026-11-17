package com.example.myshopapp.seckill;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.seckill.dto.SeckillActivityCreateRequest;
import com.example.myshopapp.seckill.dto.SeckillActivityVO;
import com.example.myshopapp.seckill.dto.SeckillStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家秒杀活动管理接口，需要 MERCHANT 或 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/merchant/seckill-activities")
@RequiredArgsConstructor
public class MerchantSeckillController {

    private final SeckillActivityService seckillActivityService;

    @GetMapping
    public ApiResponse<List<SeckillActivityVO>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(seckillActivityService.merchantList(loginUser.id()));
    }

    @PostMapping
    public ApiResponse<SeckillActivityVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                                 @Valid @RequestBody SeckillActivityCreateRequest request) {
        return ApiResponse.ok(seckillActivityService.create(loginUser.id(), request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<SeckillActivityVO> updateStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody SeckillStatusRequest request) {
        return ApiResponse.ok(seckillActivityService.updateStatus(loginUser.id(), id, request.enabled()));
    }
}
