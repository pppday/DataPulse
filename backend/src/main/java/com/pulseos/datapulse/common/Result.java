package com.pulseos.datapulse.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前后端统一 API 响应包装类。
 * <p>
 * 除文件流下载和健康检查外，全站所有 REST 接口都返回此统一结构：
 * {@code code}（业务码，200 表示成功）、{@code message}（提示语）、{@code data}（业务数据）、{@code timestamp}（服务端时间戳），
 * 方便前端 Axios 拦截器一站式做成功解包与错误弹窗。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /** 业务状态码：200 成功，400 参数/业务异常，401 未登录，403 无权限，500 系统异常 */
    private int code;
    /** 给人看的中文提示文案 */
    private String message;
    /** 实际业务响应数据载体（失败时为 null） */
    private T data;
    /** 响应生成的毫秒级时间戳，便于排查前后端时序问题 */
    private long timestamp;

    /** 构建默认成功响应（code=200，message="操作成功"） */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 构建带自定义提示语的成功响应 */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 构建指定状态码的失败响应 */
    public static <T> Result<T> error(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 构建默认 500 服务端错误响应 */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
