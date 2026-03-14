package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 资产配置项
 */
@Data
public class AssetAllocationVO {

    /**
     * 类型（基金类型或基金名称）
     */
    private String type;

    /**
     * 名称
     */
    private String name;

    /**
     * 金额
     */
    private BigDecimal value;

    /**
     * 占比
     */
    private BigDecimal ratio;

    /**
     * 颜色（用于图表）
     */
    private String color;
}
