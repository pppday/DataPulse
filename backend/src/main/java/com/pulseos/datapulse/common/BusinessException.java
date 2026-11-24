package com.pulseos.datapulse.common;

import lombok.Getter;

/**
 * 自定义业务异常类（非受检异常）。
 * <p>
 * 在 Service 或 Controller 中遇到可预期的业务校验失败（如参数非法、数据源不存在、
 * SQL 被沙箱拦截、用户名重复等）时主动抛出，最终会被 {@link GlobalExceptionHandler}
 * 统一捕获并转换为前端可直接提示的 {@link Result} JSON 结构。
 */
@Getter
public class BusinessException extends RuntimeException {
    /** 业务状态码（默认 400，也可自定义为 401/403/404 等） */
    private final int code;

    /** 默认按 400（参数或业务校验失败）构造异常 */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /** 按指定业务状态码和中文提示构造异常 */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
