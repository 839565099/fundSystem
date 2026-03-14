package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 最大回撤分析结果
 */
@Data
public class MaxDrawdownVO {
    private String fundCode;
    private String fundName;
    private String period;
    private BigDecimal maxDrawdown;
    private LocalDate peakDate;
    private LocalDate troughDate;
    private Long recoveryDays;
}
