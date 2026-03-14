package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 投资组合详情返回
 */
@Data
public class PortfolioVO {

    private Long id;

    private String name;

    private String description;

    private BigDecimal totalAmount;

    private BigDecimal currentValue;

    private BigDecimal totalProfit;

    private BigDecimal totalReturn;

    private BigDecimal dayProfit;

    private BigDecimal dayReturn;

    private Integer fundCount;

    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 持仓列表
     */
    private List<PortfolioItemVO> items;

    /**
     * 资产配置数据（用于饼图）
     */
    private List<AssetAllocationVO> allocations;
}
