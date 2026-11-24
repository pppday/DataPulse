package com.pulseos.datapulse.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常统一兜底处理器（{@code @RestControllerAdvice}）。
 * <p>
 * 拦截所有 Controller 抛出的业务异常、参数校验异常、Spring Security 认证/鉴权异常以及未知系统异常，
 * 全部翻译成前端统一约定的 {@link Result} JSON 格式，避免直接把原始堆栈吐给前端页面。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 处理业务层主动抛出的可预期异常（如登录失败、数据源连通性失败、SQL 拦截等） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务校验异常] code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 处理 {@code @Valid} / {@code @Validated} 校验 JSON 请求体失败的异常，提取第一条错误提示返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().isEmpty()
                ? "参数格式校验不合法"
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(400, msg);
    }

    /** 处理表单或 URL Query 参数绑定失败的异常 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getAllErrors().isEmpty() ? "请求参数绑定异常" : e.getAllErrors().get(0).getDefaultMessage();
        return Result.error(400, msg);
    }

    /** 处理已登录但角色权限不足（如普通分析师访问 {@code @PreAuthorize("hasRole('ADMIN')")} 接口），返回 HTTP 403 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.error(403, "操作权限不足 (403 Forbidden)：当前配置项涉及底层资产安全与语义治理，仅限企业总架构师 (ROLE_ADMIN) 修改");
    }

    /** 处理未登录或 JWT 失效导致的认证异常，返回 HTTP 401 引导前端跳回登录页 */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthException(AuthenticationException e) {
        return Result.error(401, "用户未登录或Token已过期失效");
    }

    /** 最终兜底：捕获所有未预料的系统级异常（如空指针、数据库断连），记录完整堆栈并返回 500 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("[系统异常] ", e);
        return Result.error(500, "服务端处理异常: " + e.getMessage());
    }
}
