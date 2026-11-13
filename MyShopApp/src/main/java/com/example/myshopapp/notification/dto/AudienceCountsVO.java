package com.example.myshopapp.notification.dto;

/**
 * 各发送范围的接收人数预估，便于管理员发送前确认影响面。
 */
public record AudienceCountsVO(long all, long buyer, long merchant) {
}
