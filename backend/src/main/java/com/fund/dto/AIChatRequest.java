package com.fund.dto;

import lombok.Data;

/**
 * AI 聊天请求
 */
@Data
public class AIChatRequest {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 模型类型（可选）
     */
    private String model;
}
