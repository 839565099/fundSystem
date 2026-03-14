package com.fund.vo;

import lombok.Data;
import java.util.List;

/**
 * 风险等级推荐 VO
 */
@Data
public class RiskBasedRecommendVO {
    private Integer riskLevel;
    private String riskName;
    private List<RecommendFundVO> funds;
}
