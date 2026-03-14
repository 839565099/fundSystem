package com.fund.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 聊天响应
 */
@Data
public class AIChatResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * AI 回复内容
     */
    private String content;

    /**
     * 消耗的 token 数
     */
    private Integer tokensUsed;

    /**
     * 生成时间
     */
    private LocalDateTime createTime;
}
