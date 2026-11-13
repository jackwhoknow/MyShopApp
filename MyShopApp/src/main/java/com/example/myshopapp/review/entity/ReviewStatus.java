package com.example.myshopapp.review.entity;

/**
 * 评价审核状态：命中敏感词进入待审核，其余自动通过。
 */
public enum ReviewStatus {
    /** 待人工审核，买家不可见 */
    PENDING,
    /** 审核通过，商品页展示 */
    APPROVED,
    /** 审核驳回，仅作者与商家可见 */
    REJECTED
}
