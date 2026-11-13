package com.example.myshopapp.order.entity;

/**
 * 售后单状态：买家申请 -> 商家处理。
 */
public enum AfterSaleStatus {

    /** 买家已申请，等待商家处理 */
    PENDING,
    /** 商家已同意退货，等待买家寄回 */
    RETURNING,
    /** 商家同意退款并完成 */
    REFUNDED,
    /** 商家驳回 */
    REJECTED,
    /** 平台介入关闭，售后终止且不退款 */
    CLOSED
}
