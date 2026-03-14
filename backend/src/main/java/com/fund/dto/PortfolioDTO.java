package com.fund.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建/更新投资组合请求
 */
@Data
public class PortfolioDTO {

    /**
     * 组合名称
     */
    private String name;

    /**
     * 组合描述
     */
    private String description;

    /**
     * 是否为默认组合
     */
    private Integer isDefault;
}
