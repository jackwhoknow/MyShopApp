package com.example.myshopapp.points.dto;

/**
 * 积分账户概览：余额、累计获得/使用积分与会员等级信息。
 */
public record PointAccountVO(
        int balance,
        int totalEarned,
        int totalSpent,
        String level,
        String levelName,
        String nextLevelName,
        Integer pointsToNext
) {
}
