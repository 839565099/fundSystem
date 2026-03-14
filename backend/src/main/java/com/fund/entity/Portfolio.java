package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投资组合实体
 */
@Data
@TableName("t_portfolio")
public class Portfolio implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 组合名称
     */
    private String name;

    /**
     * 组合描述
     */
    private String description;

    /**
     * 总投资金额
     */
    private BigDecimal totalAmount;

    /**
     * 当前市值
     */
    private BigDecimal currentValue;

    /**
     * 总盈亏金额
     */
    private BigDecimal totalProfit;

    /**
     * 总收益率(%)
     */
    private BigDecimal totalReturn;

    /**
     * 今日盈亏
     */
    private BigDecimal dayProfit;

    /**
     * 今日收益率(%)
     */
    private BigDecimal dayReturn;

    /**
     * 基金数量
     */
    private Integer fundCount;

    /**
     * 状态: 0-已归档 1-活跃
     */
    private Integer status;

    /**
     * 是否为默认组合
     */
    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
