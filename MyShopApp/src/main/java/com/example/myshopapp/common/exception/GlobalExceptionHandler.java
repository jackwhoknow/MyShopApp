package com.example.myshopapp.common.exception;

import com.example.myshopapp.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = HttpStatus.resolve(e.getCode());
        return ResponseEntity.status(status == null ? HttpStatus.BAD_REQUEST : status)
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    /**
     * @Valid 校验失败：只返回第一个字段错误，前端提示足够用。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /** 请求体不是合法 JSON 或字段类型无法转换：按参数错误返回，避免暴露为 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR.getCode(), "请求体格式不正确"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR.getCode(), e.getMessage()));
    }

    /** 查询参数类型不匹配（例如日期格式非法、枚举值不存在）统一按参数错误返回 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "参数 " + e.getName() + " 格式不正确";
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /** 上传文件超过 Spring 限制时返回中文提示，避免暴露为 500 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR.getCode(), "图片大小不能超过 5MB"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("未处理的异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage()));
    }
}
