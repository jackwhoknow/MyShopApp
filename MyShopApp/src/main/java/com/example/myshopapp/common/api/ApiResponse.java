package com.example.myshopapp.common.api;

/**
 * 统一响应结构：code = 0 表示成功，非 0 为业务错误码。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
