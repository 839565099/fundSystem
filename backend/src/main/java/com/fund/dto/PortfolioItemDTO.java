package com.fund.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 添加/更新持仓请求
 */
@Data
public class PortfolioItemDTO {

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 投资金额
     */
    private BigDecimal amount;

    /**
     * 持有份额
     */
    private BigDecimal shares;

    /**
     * 目标占比(%)
     */
    private BigDecimal targetRatio;

    /**
     * 买入净值
     */
    private BigDecimal buyNav;

    /**
     * 买入日期
     */
    private LocalDate buyDate;
}
