package com.example.myshopapp.notification.entity;

/**
 * 管理员群发通知的目标人群。
 */
public enum NotificationAudience {

    /** 全部正常状态的用户 */
    ALL,
    /** 仅买家 */
    BUYER,
    /** 仅商家 */
    MERCHANT
}
