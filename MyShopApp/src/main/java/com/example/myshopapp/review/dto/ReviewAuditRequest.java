package com.example.myshopapp.review.dto;

import jakarta.validation.constraints.Size;

/**
 * 评价审核备注，驳回时用于告知买家原因。
 */
public record ReviewAuditRequest(
        @Size(max = 200, message = "审核备注不能超过 200 字")
        String remark
) {
}
