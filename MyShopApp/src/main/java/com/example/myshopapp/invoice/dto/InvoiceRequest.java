package com.example.myshopapp.invoice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 申请发票入参。单位抬头必须提供税号。
 */
public record InvoiceRequest(
        @NotBlank(message = "请选择发票类型")
        @Pattern(regexp = "PERSONAL|COMPANY", message = "发票类型不正确")
        String type,

        @NotBlank(message = "请填写发票抬头")
        @Size(max = 100, message = "抬头不能超过 100 个字符")
        String title,

        @Size(max = 50, message = "税号不能超过 50 个字符")
        String taxNo,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过 100 个字符")
        String email
) {
}
