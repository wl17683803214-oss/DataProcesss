package com.example.security.handler;

import com.example.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将两个服务中的方法级权限异常统一转换为 403 响应。 */
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(
            SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>fail(
                        HttpStatus.FORBIDDEN.value(),
                        exception.getMessage()));
    }
}
