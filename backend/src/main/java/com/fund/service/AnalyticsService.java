package com.fund.service;

import com.fund.dto.CorrelationRequest;
import com.fund.dto.DIPSimRequest;
import com.fund.vo.*;

import java.util.List;

/**
 * 基金分析服务接口
 */
public interface AnalyticsService {

    /**
     * 计算夏普比率
     */
    SharpeRatioVO calculateSharpeRatio(String fundCode, String period);

    /**
     * 计算最大回撤
     */
    MaxDrawdownVO calculateMaxDrawdown(String fundCode, String period);

    /**
     * 计算波动率
     */
    VolatilityVO calculateVolatility(String fundCode, String period);

    /**
     * 计算多基金相关性矩阵
     */
    CorrelationMatrixVO calculateCorrelationMatrix(CorrelationRequest request);

    /**
     * 定投收益模拟
     */
    DIPSimResultVO simulateDIP(DIPSimRequest request);

    /**
     * 获取基金综合分析报告
     */
    FundAnalyticsReportVO getAnalyticsReport(String fundCode);

    /**
     * 批量计算并缓存基金分析指标
     */
    void batchCalculateAnalytics(List<String> fundCodes);
}
