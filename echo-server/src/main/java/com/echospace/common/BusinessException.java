package com.echospace.common;

import org.springframework.http.HttpStatus;

/**
 * 业务异常：用于表达可预期的业务错误（如账号已存在、密码错误等）。
 * <p>
 * 与 {@link IllegalArgumentException} 的区别：
 * IllegalArgumentException 表示调用方传入了非法参数（编程错误），
 * BusinessException 表示业务规则不满足（用户行为导致的正常失败路径）。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * throw new BusinessException("用户名已存在");
 * throw new BusinessException(HttpStatus.NOT_FOUND, "帖子不存在");
 * throw BusinessException.notFound("帖子不存在");
 * throw BusinessException.forbidden("无权操作他人帖子");
 * }</pre>
 *
 * @Author: taciturn-hg
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    /**
     * 默认 400 Bad Request
     *
     * @param message 业务错误描述，直接返回给前端
     */
    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    /**
     * 指定 HTTP 状态码
     *
     * @param status  HTTP 状态码
     * @param message 业务错误描述，直接返回给前端
     */
    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 获取该异常对应的 HTTP 状态码，由 {@link GlobalExceptionHandler} 读取后写入响应
     */
    public HttpStatus getStatus() {
        return status;
    }

    // ---- 工厂方法，覆盖最常用的语义 ----

    /** 资源不存在 — 404 Not Found */
    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    /** 无权限操作 — 403 Forbidden */
    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    /** 未登录 / 认证失败 — 401 Unauthorized */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, message);
    }

    /** 业务规则冲突（如重复注册）— 409 Conflict */
    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}
