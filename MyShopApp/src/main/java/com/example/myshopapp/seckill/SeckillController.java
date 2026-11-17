package com.example.myshopapp.seckill;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.seckill.dto.SeckillActivityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家秒杀专区接口：公开访问，展示进行中与即将开始的活动。
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillActivityService seckillActivityService;

    @GetMapping
    public ApiResponse<List<SeckillActivityVO>> list() {
        return ApiResponse.ok(seckillActivityService.publicList());
    }
}
