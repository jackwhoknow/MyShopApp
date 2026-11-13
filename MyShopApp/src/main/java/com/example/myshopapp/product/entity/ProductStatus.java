package com.example.myshopapp.product.entity;

/**
 * 商品状态：新建或修改后进入待审核，管理员审核通过才对前台可见。
 */
public enum ProductStatus {

    /** 草稿 */
    DRAFT,
    /** 待审核 */
    PENDING_REVIEW,
    /** 已上架（审核通过） */
    ON_SHELF,
    /** 已下架（下架或被驳回） */
    OFF_SHELF
}
