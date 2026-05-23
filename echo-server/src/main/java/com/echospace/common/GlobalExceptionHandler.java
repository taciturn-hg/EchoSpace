package com.echospace.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：统一捕获各类异常并转换为标准 {@link Result} 响应。
 * <p>
 * 处理优先级（从高到低）：
 * <ol>
 *   <li>{@link MethodArgumentNotValidException} — @Valid 参数校验失败，400</li>
 *   <li>{@link IllegalArgumentException} — 非法参数，400</li>
 *   <li>{@link BusinessException} — 业务异常，状态码由异常自身携带</li>
 *   <li>{@link Exception} — 兜底，500，仅返回通用错误信息，不暴露内部细节</li>
 * </ol>
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid 参数校验失败：将所有字段错误拼接后返回，HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.error(msg);
    }

    /**
     * 处理非法参数异常，HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 处理业务异常：HTTP 状态码由 {@link BusinessException#getStatus()} 决定
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(Result.error(e.getMessage()));
    }

    /**
     * 兜底处理：未预期的异常，HTTP 500。
     * 服务端记录完整堆栈，客户端只收到通用错误信息，不暴露内部细节。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("未预期的错误", e);
        return Result.error("服务器内部错误");
    }
}
