package com.echospace.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 全局异常处理器：统一捕获各类异常并转换为标准 {@link Result} 响应。
 * <p>
 * 处理优先级（从高到低）：
 * <ol>
 *   <li>{@link MethodArgumentNotValidException} — @Valid 参数校验失败，400</li>
 *   <li>{@link IllegalArgumentException} — 非法参数，400</li>
 *   <li>{@link MaxUploadSizeExceededException} — 上传文件超过大小限制，413</li>
 *   <li>{@link MissingServletRequestPartException} — 未携带文件，400</li>
 *   <li>{@link MultipartException} — multipart 请求解析异常，400</li>
 *   <li>{@link BusinessException} — 业务异常，状态码由异常自身携带</li>
 *   <li>{@link DuplicateKeyException} — 数据库唯一键冲突（并发写入），409</li>
 *   <li>{@link Exception} — 兜底，500，仅返回通用错误信息，不暴露内部细节</li>
 * </ol>
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MultipartProperties multipartProperties;

    public GlobalExceptionHandler(MultipartProperties multipartProperties) {
        this.multipartProperties = multipartProperties;
    }

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
     * 处理请求未携带 required multipart file part，HTTP 400
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingPart(MissingServletRequestPartException e) {
        return Result.error("请选择要上传的文件");
    }

    /**
     * 处理上传文件超过大小限制（servlet 层拦截，无法进入业务层）。
     * 映射为 HTTP 413 Content Too Large。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        DataSize maxFileSize = multipartProperties.getMaxFileSize();
        String limit = formatDataSize(maxFileSize);
        log.warn("上传文件超过大小限制 limit={}", limit);
        return Result.error("上传文件过大，当前限制为 " + limit);
    }

    private String formatDataSize(DataSize size) {
        if (size == null) return "未知";
        long bytes = size.toBytes();
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024L * 1024) return (bytes + 1024 - 1) / 1024 + "KB";
        if (bytes < 1024L * 1024 * 1024) return (bytes + 1024L * 1024 - 1) / (1024L * 1024) + "MB";
        return (bytes + 1024L * 1024 * 1024 - 1) / (1024L * 1024 * 1024) + "GB";
    }

    /**
     * 处理 multipart 请求解析异常（非 multipart 请求或格式错误），HTTP 400
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMultipart(MultipartException e) {
        log.warn("文件上传请求格式不正确", e);
        return Result.error("文件上传请求格式不正确");
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
     * 处理数据库唯一键冲突：并发写入同一唯一值时由数据库 UNIQUE 约束触发。
     * 映射为 409 Conflict，语义明确且不暴露数据库内部细节。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("数据库唯一键冲突", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.error("数据冲突，请稍后重试"));
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
