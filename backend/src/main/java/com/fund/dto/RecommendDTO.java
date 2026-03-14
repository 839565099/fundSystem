package com.fund.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 推荐请求
 */
@Data
public class RecommendDTO {

    /**
     * 风险偏好: 1-保守 2-稳健 3-激进
     */
    private Integer riskLevel;

    /**
     * 基金类型
     */
    private String fundType;

    /**
     * 最小规模
     */
    private BigDecimal minScale;

    /**
     * 最大规模
     */
    private BigDecimal maxScale;

    /**
     * 投资周期: 1-短期 2-中期 3-长期
     */
    private Integer investPeriod;
}
