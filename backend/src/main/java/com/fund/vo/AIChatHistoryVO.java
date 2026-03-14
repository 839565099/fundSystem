package com.fund.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 聊天历史 VO
 */
@Data
public class AIChatHistoryVO {

    private Long id;

    private String sessionId;

    private String role;

    private String content;

    private Integer tokensUsed;

    private LocalDateTime createTime;
}
