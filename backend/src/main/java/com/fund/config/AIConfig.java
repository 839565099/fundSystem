package com.fund.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置（Claude）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.claude")
public class AIConfig {

    /**
     * Claude API Key (x-api-key)
     */
    private String apiKey;

    /**
     * API 基础 URL
     */
    private String baseUrl = "https://api.anthropic.com";

    /**
     * 默认模型
     */
    private String defaultModel = "claude-sonnet-4-6";

    /**
     * API 版本
     */
    private String apiVersion = "2023-06-01";

    /**
     * 最大 token 数
     */
    private Integer maxTokens = 4096;

    /**
     * 温度参数
     */
    private Double temperature = 0.7;
}
