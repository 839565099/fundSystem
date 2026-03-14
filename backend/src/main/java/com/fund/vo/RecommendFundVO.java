package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 推荐基金 VO
 */
@Data
public class RecommendFundVO {

    private String fundCode;
    private String fundName;
    private String fundType;
    private BigDecimal nav;
    private BigDecimal dayGrowth;
    private BigDecimal monthGrowth;
    private BigDecimal yearGrowth;
    private BigDecimal totalGrowth;
    private BigDecimal fundScale;
    private Integer riskLevel;

    /**
     * 推荐分数
     */
    private BigDecimal score;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 推荐来源: hot/similar/personal/risk
     */
    private String source;
}
