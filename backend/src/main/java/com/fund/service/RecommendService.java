package com.fund.service;

import com.fund.dto.RecommendDTO;
import com.fund.vo.RecommendFundVO;
import com.fund.vo.RecommendOverviewVO;

import java.util.List;

/**
 * 智能推荐服务接口
 */
public interface RecommendService {

    /**
     * 获取推荐概览
     */
    RecommendOverviewVO getRecommendOverview(Long userId);

    /**
     * 获取个性化推荐
     */
    List<RecommendFundVO> getPersonalizedRecommend(Long userId, Integer limit);

    /**
     * 获取相似基金推荐
     */
    List<RecommendFundVO> getSimilarFunds(String fundCode, Integer limit);

    /**
     * 获取热门推荐
     */
    List<RecommendFundVO> getHotRecommend(Integer limit);

    /**
     * 基于风险偏好推荐
     */
    List<RecommendFundVO> getRiskBasedRecommend(RecommendDTO dto, Integer limit);

    /**
     * 记录用户行为（用于推荐算法）
     */
    void recordBehavior(Long userId, String fundCode, String behaviorType, Integer dwellTime);

    /**
     * 更新用户画像
     */
    void updateUserProfile(Long userId);

    /**
     * 计算基金相似度（定时任务）
     */
    void calculateFundSimilarity();
}
