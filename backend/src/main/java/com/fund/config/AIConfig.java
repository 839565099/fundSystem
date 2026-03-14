package com.fund.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.qwen")
public class AIConfig {

    /**
     * 通义千问 API Key
     */
    private String apiKey;

    /**
     * API 端点
     */
    private String endpoint = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    /**
     * 默认模型
     */
    private String defaultModel = "qwen-turbo";

    /**
     * 最大 token 数
     */
    private Integer maxTokens = 2000;

    /**
     * 温度参数
     */
    private Double temperature = 0.7;
}
