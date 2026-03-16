package com.fund.common;

import com.fund.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理各类异常，返回标准格式的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 获取当前请求的 Trace ID
     */
    private String getTraceId() {
        return MDC.get("traceId");
    }

    /**
     * 构建带 Trace ID 的日志前缀
     */
    private String logPrefix() {
        String traceId = getTraceId();
        return traceId != null ? "[" + traceId + "] " : "";
    }

    // ==================== 业务异常 ====================

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("{}业务异常: {}", logPrefix(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    /**
     * @Valid 参数校验失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("{}参数验证失败: {}", logPrefix(), message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("{}参数绑定失败: {}", logPrefix(), message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * @Validated 单参数校验失败异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
        log.warn("{}约束校验失败: {}", logPrefix(), message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 缺少必需参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParameterException(MissingServletRequestParameterException e) {
        String message = "缺少必需参数: " + e.getParameterName();
        log.warn("{}{}", logPrefix(), message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatchException(Exception e) {
        String message = "参数类型错误";
        if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException ex = (MethodArgumentTypeMismatchException) e;
            message = "参数 '" + ex.getName() + "' 类型错误，期望类型: " + ex.getRequiredType();
        }
        log.warn("{}{}", logPrefix(), message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 请求体解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("{}请求体解析失败: {}", logPrefix(), e.getMessage());
        return Result.error(ErrorCode.MESSAGE_NOT_READABLE.getCode(), ErrorCode.MESSAGE_NOT_READABLE.getMessage());
    }

    // ==================== HTTP 相关异常 ====================

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = "不支持的请求方法: " + e.getMethod() +
            "，支持的方法: " + String.join(", ", e.getSupportedMethods());
        log.warn("{}{}", logPrefix(), message);
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED.getCode(), message);
    }

    /**
     * 媒体类型不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        String message = "不支持的媒体类型: " + e.getContentType();
        log.warn("{}{}", logPrefix(), message);
        return Result.error(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED.getCode(), ErrorCode.MEDIA_TYPE_NOT_SUPPORTED.getMessage());
    }

    /**
     * 404 处理器未找到
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String message = "接口不存在: " + e.getRequestURL();
        log.warn("{}{}", logPrefix(), message);
        return Result.error(ErrorCode.NOT_FOUND.getCode(), message);
    }

    // ==================== 请求超时异常 ====================

    /**
     * 异步请求超时
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public Result<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.warn("{}异步请求超时: {}", logPrefix(), e.getMessage());
        return Result.error(ErrorCode.REQUEST_TIMEOUT.getCode(), ErrorCode.REQUEST_TIMEOUT.getMessage());
    }

    // ==================== 数据库异常 ====================

    /**
     * 数据库访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleDataAccessException(DataAccessException e) {
        log.error("{}数据库访问异常: {}", logPrefix(), e.getMessage(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "数据操作失败，请稍后重试");
    }

    // ==================== 兜底异常处理 ====================

    /**
     * 其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("{}系统异常: {}", logPrefix(), e.getMessage(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常，请稍后重试");
    }

    /**
     * 空指针异常（特殊处理，避免暴露敏感信息）
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("{}空指针异常: ", logPrefix(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部错误，请联系管理员");
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("{}非法参数: {}", logPrefix(), e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.error("{}非法状态: {}", logPrefix(), e.getMessage(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统状态异常，请稍后重试");
    }
}
