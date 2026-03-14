package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 夏普比率分析结果
 */
@Data
public class SharpeRatioVO {
    private String fundCode;
    private String fundName;
    private String period;
    private BigDecimal sharpeRatio;
    private BigDecimal avgReturn;
    private BigDecimal volatility;
    private BigDecimal riskFreeRate;
}
