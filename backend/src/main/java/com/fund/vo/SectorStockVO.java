package com.fund.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 板块成分股VO
 */
@Data
public class SectorStockVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 股票代码 */
    private String code;

    /** 股票名称 */
    private String name;

    /** 涨跌幅 */
    private BigDecimal changePercent;

    /** 现价 */
    private BigDecimal price;

    /** 成交额（亿） */
    private BigDecimal volume;

    /** 是否为领涨股 */
    private Boolean isLeading;
}
