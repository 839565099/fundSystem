package com.fund.vo;

import lombok.Data;
import java.util.List;

/**
 * 推荐概览 VO
 */
@Data
public class RecommendOverviewVO {

    /**
     * 个性化推荐
     */
    private List<RecommendFundVO> personalized;

    /**
     * 热门推荐
     */
    private List<RecommendFundVO> hot;

    /**
     * 相似基金
     */
    private SimilarFundsVO similar;

    /**
     * 按风险等级推荐
     */
    private RiskBasedRecommendVO riskBased;
}
