package com.example.myshopapp.common.exception;

/**
 * 业务异常：由 Service 层主动抛出，交给全局异常处理器转成统一响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
