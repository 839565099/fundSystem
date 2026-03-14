package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 定投模拟结果
 */
@Data
public class DIPSimResultVO {
    private String fundCode;
    private String fundName;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal totalProfit;
    private BigDecimal totalReturn;
    private Integer totalShares;
    private BigDecimal avgCost;
    private List<MonthlyRecord> monthlyRecords;

    @Data
    public static class MonthlyRecord {
        private String month;
        private BigDecimal invested;
        private BigDecimal shares;
        private BigDecimal nav;
        private BigDecimal value;
    }
}
