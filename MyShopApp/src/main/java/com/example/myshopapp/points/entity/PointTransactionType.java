package com.example.myshopapp.points.entity;

/**
 * 积分流水类型：下单抵扣、支付赠送、取消退回、整单退款冲正、签到奖励、积分兑换。
 */
public enum PointTransactionType {
    ORDER_DEDUCT,
    ORDER_EARN,
    ORDER_CANCEL_RETURN,
    ORDER_REFUND,
    CHECKIN,
    POINT_EXCHANGE
}
