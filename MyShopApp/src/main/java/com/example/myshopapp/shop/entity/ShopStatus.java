package com.example.myshopapp.shop.entity;

/**
 * 店铺状态：提交申请后待审核，审核通过才可发布商品。
 */
public enum ShopStatus {

    /** 待审核 */
    PENDING,
    /** 正常营业 */
    ACTIVE,
    /** 审核被驳回 */
    REJECTED,
    /** 已关闭 */
    CLOSED
}
