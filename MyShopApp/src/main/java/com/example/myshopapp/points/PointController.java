package com.example.myshopapp.points;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.points.dto.CheckinStatusVO;
import com.example.myshopapp.points.dto.PointAccountVO;
import com.example.myshopapp.points.dto.PointCouponVO;
import com.example.myshopapp.points.dto.PointQuoteVO;
import com.example.myshopapp.points.dto.PointTransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 买家积分接口：账户概览、流水分页、抵扣试算、签到与积分兑换。
 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final CheckinService checkinService;
    private final PointMallService pointMallService;

    @GetMapping("/account")
    public ApiResponse<PointAccountVO> account(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(pointService.account(loginUser.id()));
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResult<PointTransactionVO>> transactions(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(pointService.transactions(loginUser.id(), page, size));
    }

    /** 抵扣试算：amount 传入扣券后的应付金额 */
    @GetMapping("/quote")
    public ApiResponse<PointQuoteVO> quote(@AuthenticationPrincipal LoginUser loginUser,
                                           @RequestParam BigDecimal amount) {
        return ApiResponse.ok(pointService.quote(loginUser.id(), amount));
    }

    @GetMapping("/checkin")
    public ApiResponse<CheckinStatusVO> checkinStatus(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(checkinService.status(loginUser.id()));
    }

    @PostMapping("/checkin")
    public ApiResponse<CheckinStatusVO> checkin(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(checkinService.checkin(loginUser.id()));
    }

    /** 积分兑换专区 */
    @GetMapping("/coupons")
    public ApiResponse<List<PointCouponVO>> exchangeableCoupons(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(pointMallService.available(loginUser.id()));
    }

    @PostMapping("/coupons/{couponId}/exchange")
    public ApiResponse<PointCouponVO> exchange(@AuthenticationPrincipal LoginUser loginUser,
                                               @PathVariable Long couponId) {
        return ApiResponse.ok(pointMallService.exchange(loginUser.id(), couponId));
    }
}
