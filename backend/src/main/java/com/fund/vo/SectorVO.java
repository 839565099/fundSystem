package com.fund.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 板块数据VO
 */
@Data
public class SectorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 板块代码 */
    private String code;

    /** 板块名称 */
    private String name;

    /** 板块类型: industry-行业, concept-概念, region-地域 */
    private String type;

    /** 当前点位 */
    private BigDecimal price;

    /** 涨跌额 */
    private BigDecimal change;

    /** 今日涨跌幅 */
    private BigDecimal changePercent;

    /** 周涨跌幅 */
    private BigDecimal weekGrowth;

    /** 月涨跌幅 */
    private BigDecimal monthGrowth;

    /** 三月涨跌幅 */
    private BigDecimal threeMonthGrowth;

    /** 六月涨跌幅 */
    private BigDecimal sixMonthGrowth;

    /** 年涨跌幅 */
    private BigDecimal yearGrowth;

    /** 成交额（亿） */
    private BigDecimal volume;

    /** 换手率 */
    private BigDecimal turnover;

    /** 领涨股名称 */
    private String leadingStock;

    /** 领涨股代码 */
    private String leadingStockCode;

    /** 领涨股涨幅 */
    private BigDecimal leadingStockGrowth;

    /** 成分股数量 */
    private Integer stockCount;

    /** 上涨家数 */
    private Integer upCount;

    /** 下跌家数 */
    private Integer downCount;

    /** 数据更新时间 */
    private LocalDateTime updateTime;
}
