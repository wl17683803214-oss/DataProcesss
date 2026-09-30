package com.example.simulator.api;

import com.example.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 将可预期错误转为中文响应，数据库细节仅保留在日志中。 */
@RestControllerAdvice
public class SimulatorExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorExceptionHandler.class);

    /** 请求校验统一使用中文提示，避免框架默认英文消息。 */
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, "请求字段缺失、格式错误或超出允许范围"));
    }

    /** 参数和业务状态错误分别返回明确状态码。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> invalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, exception.getMessage()));
    }

    /** 状态竞争或重复启动返回冲突。 */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(409).body(ApiResponse.fail(409, exception.getMessage()));
    }

    /** 唯一约束冲突不能暴露数据库原始错误。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> databaseConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(ApiResponse.fail(409, "名称重复、模拟源已经启动或配置违反数据库约束"));
    }

    /** 未预期错误记录中文上下文，调用方收到统一错误。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        LOGGER.error("模拟源接口处理失败", exception);
        return ResponseEntity.status(500).body(ApiResponse.fail(500, "模拟源服务处理失败，请检查服务日志"));
    }
}

