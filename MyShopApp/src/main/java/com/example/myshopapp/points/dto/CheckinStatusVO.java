package com.example.myshopapp.points.dto;

/**
 * 签到状态：今日是否已签、连续天数与本次/下次签到奖励。
 */
public record CheckinStatusVO(
        boolean checkedInToday,
        int consecutiveDays,
        int todayPoints,
        int nextPoints
) {
}
