package com.echospace.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一 API 响应包装类
 * <p>
 * 所有接口均返回此结构：code=1 表示成功，code=0 表示失败。
 * data 在成功时携带业务数据，失败时为 null；msg 在失败时携带错误描述。
 * </p>
 *
 * @param <T> 业务数据类型
 * @Author: taciturn-hg
 */
@Schema(description = "统一响应结构")
public class Result<T> {

    @Schema(description = "响应码：1=成功，0=失败", example = "1")
    private int code;

    @Schema(description = "提示信息", example = "success")
    private String msg;

    @Schema(description = "业务数据，失败时为 null")
    private T data;

    private Result() {}

    /**
     * 成功响应，携带业务数据
     *
     * @param data 业务数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = "success";
        r.data = data;
        return r;
    }

    /**
     * 成功响应，携带自定义提示信息（无 data，适用于注册、删除等操作）
     *
     * @param msg 提示信息，如"注册成功"
     */
    public static <T> Result<T> success(String msg) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = msg;
        return r;
    }

    /**
     * 成功响应，无数据无自定义 msg
     */
    public static <T> Result<T> success() {
        return success((T) null);
    }

    /**
     * 成功响应，携带业务数据和自定义提示信息
     *
     * @param data 业务数据
     * @param msg  提示信息，如"发布成功"
     */
    public static <T> Result<T> success(T data, String msg) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = msg;
        r.data = data;
        return r;
    }

    /**
     * 失败响应
     *
     * @param msg 错误描述，直接展示给前端
     */
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = msg;
        return r;
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }
}
