package com.example.myshopapp.order.entity;

/**
 * 售后类型：未发货走仅退款，已发货/已完成走退货退款。
 */
public enum AfterSaleType {

    /** 仅退款：发货前申请，商家同意后直接退款 */
    REFUND_ONLY,
    /** 退货退款：发货后申请，买家寄回后商家确认收货再退款 */
    RETURN_REFUND
}
