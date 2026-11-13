package com.example.myshopapp.order.entity;

/**
 * 子订单状态：按店铺拆单后，每个店铺独立履约。
 */
public enum SubOrderStatus {

    WAIT_SHIP,
    /** 买家已申请退款，商家处理中，此状态不可发货 */
    REFUNDING,
    SHIPPED,
    COMPLETED,
    CANCELLED
}
