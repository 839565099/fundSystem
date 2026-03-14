package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 波动率分析结果
 */
@Data
public class VolatilityVO {
    private String fundCode;
    private String fundName;
    private String period;
    private BigDecimal dailyVolatility;
    private BigDecimal annualizedVolatility;
    private BigDecimal downsideVolatility;
}
