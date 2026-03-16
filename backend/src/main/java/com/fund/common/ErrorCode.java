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
    TOKEN_INVALID(4002, "Token无效"),

    // 投资组合相关错误码 5xxx
    PORTFOLIO_NOT_FOUND(5001, "投资组合不存在"),
    PORTFOLIO_NAME_EXISTS(5002, "组合名称已存在"),
    PORTFOLIO_ITEM_LIMIT(5003, "组合持仓数量已达上限"),
    PORTFOLIO_ITEM_NOT_FOUND(5004, "组合持仓项不存在"),
    PORTFOLIO_ACCESS_DENIED(5005, "无权访问该组合"),

    // 预警相关错误码 6xxx
    ALERT_RULE_NOT_FOUND(6001, "预警规则不存在"),
    ALERT_RULE_LIMIT(6002, "预警规则数量已达上限"),
    ALERT_RULE_INVALID(6003, "预警规则参数无效"),

    // AI助手相关错误码 7xxx
    AI_SERVICE_UNAVAILABLE(7001, "AI服务暂不可用"),
    AI_SESSION_NOT_FOUND(7002, "AI会话不存在"),
    AI_API_KEY_NOT_CONFIGURED(7003, "AI服务未配置"),
    AI_REQUEST_FAILED(7004, "AI请求失败"),

    // 通用错误码 8xxx
    RESOURCE_NOT_FOUND(8001, "请求的资源不存在"),
    RATE_LIMIT_EXCEEDED(8002, "请求过于频繁，请稍后重试"),
    REQUEST_TIMEOUT(8003, "请求超时"),
    SERVICE_UNAVAILABLE(8004, "服务暂不可用"),
    MEDIA_TYPE_NOT_SUPPORTED(8005, "不支持的媒体类型"),
    MESSAGE_NOT_READABLE(8006, "请求数据格式错误");

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
