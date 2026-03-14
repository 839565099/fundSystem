package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 基金综合分析报告
 */
@Data
public class FundAnalyticsReportVO {
    private String fundCode;
    private String fundName;
    private String fundType;

    // 基础指标
    private BigDecimal currentNav;
    private BigDecimal dayGrowth;
    private BigDecimal monthGrowth;
    private BigDecimal yearGrowth;
    private BigDecimal totalGrowth;

    // 风险指标
    private BigDecimal sharpeRatio;
    private BigDecimal maxDrawdown;
    private BigDecimal volatility;
    private Integer riskLevel;

    // 评级
    private String performanceRating;
    private String riskRating;
    private String overallRating;

    // 建议
    private String investmentAdvice;
}
