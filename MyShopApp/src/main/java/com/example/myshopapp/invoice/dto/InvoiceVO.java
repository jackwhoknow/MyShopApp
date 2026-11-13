package com.example.myshopapp.invoice.dto;

import com.example.myshopapp.invoice.entity.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceVO(
        Long id,
        Long orderId,
        String type,
        String title,
        String taxNo,
        String email,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime issuedAt
) {

    public static InvoiceVO from(Invoice invoice) {
        return new InvoiceVO(
                invoice.getId(),
                invoice.getOrderId(),
                invoice.getType(),
                invoice.getTitle(),
                invoice.getTaxNo(),
                invoice.getEmail(),
                invoice.getAmount(),
                invoice.getStatus(),
                invoice.getCreatedAt(),
                invoice.getIssuedAt());
    }
}
