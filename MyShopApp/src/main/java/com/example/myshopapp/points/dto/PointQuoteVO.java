package com.example.myshopapp.points.dto;

import java.math.BigDecimal;

/**
 * 结算页积分抵扣试算：可抵扣积分必须为 100 的整数倍。
 */
public record PointQuoteVO(int balance, int maxDeductiblePoints, BigDecimal maxDeductibleAmount) {
}
