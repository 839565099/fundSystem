package com.fund.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 会话 VO
 */
@Data
public class AIChatSessionVO {

    private Long id;

    private String sessionId;

    private String title;

    private String modelType;

    private Integer totalTokens;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
