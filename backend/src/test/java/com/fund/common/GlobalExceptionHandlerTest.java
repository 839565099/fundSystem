package com.fund.common;

import com.fund.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器测试
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    @DisplayName("处理业务异常")
    void handleBusinessException() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

        // when
        Result<Void> result = exceptionHandler.handleBusinessException(exception);

        // then
        assertNotNull(result);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), result.getCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), result.getMessage());
    }

    @Test
    @DisplayName("处理业务异常 - 自定义消息")
    void handleBusinessException_customMessage() {
        // given
        BusinessException exception = new BusinessException("自定义错误消息");

        // when
        Result<Void> result = exceptionHandler.handleBusinessException(exception);

        // then
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("自定义错误消息", result.getMessage());
    }

    @Test
    @DisplayName("处理参数校验异常 - 使用 BindException")
    void handleBindException() {
        // given - 使用 BindException 替代 MethodArgumentNotValidException 进行测试
        org.springframework.validation.BindException bindException =
            new org.springframework.validation.BindException(new Object(), "target");
        bindException.addError(new FieldError("target", "username", "用户名不能为空"));

        // when
        Result<Void> result = exceptionHandler.handleBindException(bindException);

        // then
        assertNotNull(result);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("用户名不能为空"));
    }

    @Test
    @DisplayName("处理缺少必需参数异常")
    void handleMissingParameterException() {
        // given
        MissingServletRequestParameterException exception =
            new MissingServletRequestParameterException("userId", "Long");

        // when
        Result<Void> result = exceptionHandler.handleMissingParameterException(exception);

        // then
        assertNotNull(result);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("userId"));
    }

    @Test
    @DisplayName("处理未知异常")
    void handleException() {
        // given
        Exception exception = new RuntimeException("未知错误");

        // when
        Result<Void> result = exceptionHandler.handleException(exception);

        // then
        assertNotNull(result);
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), result.getCode());
        assertEquals("系统异常，请稍后重试", result.getMessage());
    }

    @Test
    @DisplayName("处理空指针异常")
    void handleNullPointerException() {
        // given
        NullPointerException exception = new NullPointerException("null reference");

        // when
        Result<Void> result = exceptionHandler.handleNullPointerException(exception);

        // then
        assertNotNull(result);
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), result.getCode());
        assertEquals("系统内部错误，请联系管理员", result.getMessage());
    }

    @Test
    @DisplayName("处理非法参数异常")
    void handleIllegalArgumentException() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("参数不合法");

        // when
        Result<Void> result = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertNotNull(result);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), result.getCode());
        assertEquals("参数不合法", result.getMessage());
    }

    @Test
    @DisplayName("带 TraceId 的异常处理")
    void handleException_withTraceId() {
        // given
        String traceId = "test-trace-id-12345";
        MDC.put("traceId", traceId);
        BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

        // when
        Result<Void> result = exceptionHandler.handleBusinessException(exception);

        // then
        assertNotNull(result);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), result.getCode());

        // cleanup
        MDC.remove("traceId");
    }
}
