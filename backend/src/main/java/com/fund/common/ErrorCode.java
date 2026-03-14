package com.fund.common;

public enum ErrorCode {
    
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已被禁用"),
    USERNAME_EXISTS(1004, "用户名已存在"),
    EMAIL_EXISTS(1005, "邮箱已被注册"),
    PHONE_EXISTS(1006, "手机号已被注册"),
    
    FUND_NOT_FOUND(2001, "基金不存在"),
    FUND_CODE_ERROR(2002, "基金代码格式错误"),
    
    FAVORITE_EXISTS(3001, "已收藏该基金"),
    FAVORITE_LIMIT(3002, "收藏数量已达上限"),
    
    TOKEN_EXPIRED(4001, "Token已过期"),
    TOKEN_INVALID(4002, "Token无效");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
