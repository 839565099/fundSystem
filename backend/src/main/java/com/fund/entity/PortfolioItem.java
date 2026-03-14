package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组合持仓实体
 */
@Data
@TableName("t_portfolio_item")
public class PortfolioItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 组合ID
     */
    private Long portfolioId;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称(冗余)
     */
    private String fundName;

    /**
     * 持有份额
     */
    private BigDecimal shares;

    /**
     * 投资金额
     */
    private BigDecimal amount;

    /**
     * 目标占比(%)
     */
    private BigDecimal targetRatio;

    /**
     * 实际占比(%)
     */
    private BigDecimal actualRatio;

    /**
     * 买入净值
     */
    private BigDecimal buyNav;

    /**
     * 买入日期
     */
    private LocalDate buyDate;

    /**
     * 当前净值
     */
    private BigDecimal currentNav;

    /**
     * 当前市值
     */
    private BigDecimal currentValue;

    /**
     * 盈亏金额
     */
    private BigDecimal profit;

    /**
     * 盈亏比例(%)
     */
    private BigDecimal profitRatio;

    /**
     * 今日盈亏
     */
    private BigDecimal dayProfit;

    /**
     * 状态: 0-已清仓 1-持有
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
