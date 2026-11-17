package com.example.myshopapp.notification.entity;

/**
 * 站内通知类型，前端按类型展示不同图标与颜色。
 */
public enum NotificationType {

    /** 买家支付成功 */
    ORDER_PAID,
    /** 商家收到新订单 */
    NEW_ORDER,
    /** 商家已发货 */
    ORDER_SHIPPED,
    /** 买家确认收货 */
    ORDER_COMPLETED,
    /** 订单被取消 */
    ORDER_CANCELLED,
    /** 商家收到售后申请 */
    AFTER_SALE_APPLIED,
    /** 商家同意退货，等待买家寄回 */
    AFTER_SALE_APPROVED,
    /** 售后申请被驳回 */
    AFTER_SALE_REJECTED,
    /** 买家已寄回退货 */
    AFTER_SALE_RETURN_SHIPPED,
    /** 售后退款完成 */
    AFTER_SALE_REFUNDED,
    /** 平台介入处理售后 */
    AFTER_SALE_PLATFORM_INTERVENED,
    /** 提现审核通过，等待打款 */
    WITHDRAWAL_APPROVED,
    /** 提现申请被驳回 */
    WITHDRAWAL_REJECTED,
    /** 提现已打款 */
    WITHDRAWAL_PAID,
    /** 管理员群发通知 */
    ADMIN_BROADCAST,
    /** 发货超时系统自动确认收货 */
    ORDER_AUTO_COMPLETED,
    /** 优惠券即将到期提醒 */
    COUPON_EXPIRING,
    /** 会员等级提升 */
    MEMBER_LEVEL_UP,
    /** 每日签到奖励 */
    CHECKIN_REWARD,
    /** 收到客服消息 */
    SUPPORT_MESSAGE,
    /** 售后超时未处理被系统关闭 */
    AFTER_SALE_AUTO_CLOSED
}
