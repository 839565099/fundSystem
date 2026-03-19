package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组合持仓详情
 */
@Data
public class PortfolioItemVO {

    private Long id;

    private String fundCode;

    private String fundName;

    private String fundType;

    private BigDecimal shares;

    private BigDecimal amount;

    private BigDecimal targetRatio;

    private BigDecimal actualRatio;

    private BigDecimal buyNav;

    private BigDecimal currentNav;

    private BigDecimal currentValue;

    private BigDecimal profit;

    private BigDecimal profitRatio;

    private BigDecimal dayProfit;

    private BigDecimal dayGrowth;

    /**
     * 昨日盈亏
     */
    private BigDecimal yesterdayProfit;

    /**
     * 昨日涨跌幅(%)
     */
    private BigDecimal yesterdayGrowth;

    private LocalDate buyDate;

    private Integer status;

    private LocalDateTime updateTime;
}
