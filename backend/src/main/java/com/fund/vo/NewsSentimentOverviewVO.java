package com.fund.vo;

import lombok.Data;

@Data
public class NewsSentimentOverviewVO {

    private Long bullishCount;

    private Long bearishCount;

    private Long neutralCount;

    /**
     * 利好百分比
     */
    private Double bullishPercent;

    /**
     * 利空百分比
     */
    private Double bearishPercent;

    /**
     * 中性百分比
     */
    private Double neutralPercent;

    /**
     * 情绪指数：(bullish - bearish) / total
     */
    private Double sentimentIndex;

    private Integer minutes;

    private String fundCode;
}
